#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <cstring>
#include <cmath>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/channel_layout.h>
#include <libavutil/display.h>
#include <libavutil/error.h>
#include <libavutil/imgutils.h>
#include <libavutil/opt.h>
#include <libavutil/time.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>
}

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FfmpegPlayer", __VA_ARGS__)

/** 视频帧落后主时钟超过该值时丢弃，用于追赶音频 */
static const int64_t SYNC_DROP_THRESHOLD_MS = 80;
/** 时间戳跳变阈值，超过该值认为播放时钟需要重建 */
static const int64_t SYNC_RESET_THRESHOLD_MS = 2000;
/** 主时钟超过该时长未更新视为失效，退化为视频自身时钟 */
static const int64_t MASTER_CLOCK_TIMEOUT_US = 1000000;
/** 同步等待的分片时长，保证能及时响应暂停、seek 与倍速切换 */
static const int64_t SYNC_SLEEP_SLICE_US = 20000;
/** 连续丢帧上限，避免长时间不刷新画面 */
static const int MAX_CONTINUOUS_DROP_FRAMES = 15;
/** 暂停状态下 seek 时，为对齐目标位置最多丢弃的帧数 */
static const int MAX_SEEK_SKIP_FRAMES = 600;

static JavaVM *g_vm = nullptr;

static float clamp_speed(float speed) {
    return speed > 0.0f ? speed : 1.0f;
}

static std::string ff_error(int code) {
    char buffer[AV_ERROR_MAX_STRING_SIZE] = {0};
    av_strerror(code, buffer, sizeof(buffer));
    return buffer;
}

static JNIEnv *attach_env(bool *attached) {
    *attached = false;
    JNIEnv *env = nullptr;
    if (g_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) == JNI_OK) {
        return env;
    }
    if (g_vm->AttachCurrentThread(&env, nullptr) == JNI_OK) {
        *attached = true;
        return env;
    }
    return nullptr;
}

static void detach_env(bool attached) {
    if (attached) {
        g_vm->DetachCurrentThread();
    }
}

struct BasePlayer {
    jobject owner = nullptr;
    jmethodID on_completion = nullptr;
    jmethodID on_error = nullptr;
    jmethodID on_progress = nullptr;
    std::string source;
    std::thread worker;
    std::mutex mutex;
    std::condition_variable cv;
    std::atomic_bool stop_requested{false};
    std::atomic_bool pause_requested{false};
    std::atomic_bool running{false};
    std::atomic<float> playback_speed{1.0f};
    std::atomic<int64_t> current_position_ms{0};
    std::atomic<int64_t> duration_ms{0};
    std::atomic<int64_t> seek_request_ms{-1};
    std::atomic<int64_t> last_progress_callback_ms{-1};
    /** seek 请求序号，与 seek_done_serial 不相等表示解码器尚未完成定位 */
    std::atomic<int64_t> seek_serial{0};
    std::atomic<int64_t> seek_done_serial{0};
    /** 需要重建播放时钟（首帧、seek、恢复播放、倍速切换） */
    std::atomic_bool clock_rebase_requested{true};

    virtual ~BasePlayer() = default;

    /** seek 被解码线程消费后的回调，供子类同步输出设备状态 */
    virtual void onSeekConsumed(int64_t /* position_ms */, bool /* success */) {}

    /** 解码线程结束后的回调，供子类清理播放时钟等状态 */
    virtual void onStopped() {}

    void notifyCompletion() {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_completion != nullptr) {
            env->CallVoidMethod(owner, on_completion);
        }
        detach_env(attached);
    }

    void notifyError(int code, const std::string &message) {
        LOGE("error %d: %s", code, message.c_str());
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_error != nullptr) {
            jstring j_message = env->NewStringUTF(message.c_str());
            env->CallVoidMethod(owner, on_error, code, j_message);
            env->DeleteLocalRef(j_message);
        }
        detach_env(attached);
    }

    void notifyProgress(bool force = false) {
        int64_t position = current_position_ms.load();
        int64_t duration = duration_ms.load();
        int64_t last_progress = last_progress_callback_ms.load();
        if (!force && last_progress >= 0 &&
            std::llabs(position - last_progress) < 500) {
            return;
        }
        last_progress_callback_ms = position;

        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_progress != nullptr) {
            env->CallVoidMethod(owner, on_progress,
                                static_cast<jlong>(position),
                                static_cast<jlong>(duration));
        }
        detach_env(attached);
    }

    /** 暂停期间阻塞解码线程，收到停止或 seek 请求时立即唤醒 */
    void waitForResumeOrSeek() {
        std::unique_lock<std::mutex> lock(mutex);
        cv.wait(lock, [&] {
            return stop_requested.load() || !pause_requested.load() ||
                   seek_request_ms.load() >= 0;
        });
    }

    void requestSeek(int64_t position_ms) {
        std::lock_guard<std::mutex> lock(mutex);
        // 先自增序号再挂请求，保证解码线程取到请求时一定能读到对应的序号
        seek_serial.fetch_add(1);
        seek_request_ms = position_ms;
        current_position_ms = position_ms;
        clock_rebase_requested = true;
        cv.notify_all();
    }

    bool isSeeking() const {
        return seek_done_serial.load() != seek_serial.load();
    }

    void resetSeekState() {
        seek_request_ms = -1;
        seek_serial = 0;
        seek_done_serial = 0;
    }
};

struct VideoPlayer : BasePlayer {
    jmethodID on_prepared = nullptr;
    jmethodID on_video_size = nullptr;

    /** 渲染窗口可在播放过程中被替换（Surface 重建），访问需持锁 */
    std::mutex window_mutex;
    ANativeWindow *window = nullptr;
    bool window_dirty = false;
    int display_width = 0;
    int display_height = 0;

    /** 由上层（音频输出进度）推送的主时钟锚点 */
    std::atomic<int64_t> master_clock_ms{0};
    std::atomic<int64_t> master_clock_time_us{0};
    /** 无音频轨时使用的视频自身时钟锚点，仅解码线程访问 */
    int64_t video_clock_pts_ms = 0;
    int64_t video_clock_time_us = 0;

    void onStopped() override {
        master_clock_time_us = 0;
        master_clock_ms = 0;
        video_clock_time_us = 0;
        video_clock_pts_ms = 0;
        clock_rebase_requested = true;
    }

    void notifyPrepared() {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_prepared != nullptr) {
            env->CallVoidMethod(owner, on_prepared);
        }
        detach_env(attached);
    }

    void notifyVideoSize(int width, int height) {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_video_size != nullptr) {
            env->CallVoidMethod(owner, on_video_size, width, height);
        }
        detach_env(attached);
    }
};

struct AudioPlayer : BasePlayer {
    jmethodID on_audio_format = nullptr;
    jmethodID on_audio_data = nullptr;
    jmethodID on_audio_flush = nullptr;
    jmethodID on_audio_unavailable = nullptr;

    /** 解码器完成定位后通知上层丢弃 AudioTrack 中的旧数据并重建输出时钟 */
    void notifyAudioFlush(int64_t position_ms) {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_audio_flush != nullptr) {
            env->CallVoidMethod(owner, on_audio_flush, static_cast<jlong>(position_ms));
        }
        detach_env(attached);
    }

    /** 媒体中不存在可解码的音频轨 */
    void notifyAudioUnavailable() {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_audio_unavailable != nullptr) {
            env->CallVoidMethod(owner, on_audio_unavailable);
        }
        detach_env(attached);
    }

    void onSeekConsumed(int64_t position_ms, bool /* success */) override {
        notifyAudioFlush(position_ms);
    }

    void notifyAudioFormat(int sample_rate, int channels) {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_audio_format != nullptr) {
            env->CallVoidMethod(owner, on_audio_format, sample_rate, channels);
        }
        detach_env(attached);
    }

    void notifyAudioData(const uint8_t *data, int size, int64_t pts_ms) {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_audio_data != nullptr && size > 0) {
            jbyteArray array = env->NewByteArray(size);
            env->SetByteArrayRegion(array, 0, size, reinterpret_cast<const jbyte *>(data));
            env->CallVoidMethod(owner, on_audio_data, array, static_cast<jlong>(pts_ms));
            env->DeleteLocalRef(array);
        }
        detach_env(attached);
    }
};

struct MediaContext {
    AVFormatContext *format = nullptr;
    AVCodecContext *codec = nullptr;
    int stream_index = -1;

    ~MediaContext() {
        if (codec != nullptr) {
            avcodec_free_context(&codec);
        }
        if (format != nullptr) {
            avformat_close_input(&format);
        }
    }
};

static int64_t read_duration_ms(AVFormatContext *format, int stream_index) {
    if (format == nullptr) {
        return 0;
    }
    if (format->duration > 0) {
        return av_rescale(format->duration, 1000, AV_TIME_BASE);
    }
    if (stream_index >= 0 && stream_index < static_cast<int>(format->nb_streams)) {
        AVStream *stream = format->streams[stream_index];
        if (stream != nullptr && stream->duration > 0) {
            return av_rescale_q(stream->duration, stream->time_base, AVRational{1, 1000});
        }
    }
    return 0;
}

static int64_t frame_position_ms(const AVFrame *frame, const AVStream *stream) {
    if (frame == nullptr || stream == nullptr) {
        return 0;
    }
    int64_t pts = frame->best_effort_timestamp;
    if (pts == AV_NOPTS_VALUE) {
        pts = frame->pts;
    }
    if (pts == AV_NOPTS_VALUE) {
        return 0;
    }
    return av_rescale_q(pts, stream->time_base, AVRational{1, 1000});
}

static bool perform_seek(MediaContext *media, int64_t position_ms) {
    if (media == nullptr || media->format == nullptr || media->stream_index < 0) {
        return false;
    }
    AVStream *stream = media->format->streams[media->stream_index];
    int64_t target = av_rescale_q(position_ms, AVRational{1, 1000}, stream->time_base);
    int ret = av_seek_frame(media->format, media->stream_index, target, AVSEEK_FLAG_BACKWARD);
    if (ret < 0) {
        ret = avformat_seek_file(media->format, media->stream_index, INT64_MIN, target, INT64_MAX, 0);
    }
    if (ret >= 0 && media->codec != nullptr) {
        avcodec_flush_buffers(media->codec);
        return true;
    }
    return false;
}

/** 消费一次待处理的 seek，返回是否发生过定位 */
static bool consume_pending_seek(BasePlayer *player, MediaContext *media) {
    int64_t requested = player->seek_request_ms.exchange(-1);
    if (requested < 0) {
        return false;
    }
    int64_t serial = player->seek_serial.load();
    bool success = perform_seek(media, requested);
    player->current_position_ms = requested;
    player->clock_rebase_requested = true;
    player->onSeekConsumed(requested, success);
    // 无论成功与否都推进完成序号，避免上层进度长期停留在拖动目标上
    player->seek_done_serial = serial;
    player->notifyProgress(true);
    if (!success) {
        player->notifyError(-3, "Seek failed.");
    }
    return true;
}

static int open_media(const std::string &source, AVMediaType type, MediaContext *ctx) {
    AVDictionary *options = nullptr;
    av_dict_set(&options, "reconnect", "1", 0);
    av_dict_set(&options, "reconnect_streamed", "1", 0);
    av_dict_set(&options, "reconnect_delay_max", "5", 0);

    int ret = avformat_open_input(&ctx->format, source.c_str(), nullptr, &options);
    av_dict_free(&options);
    if (ret < 0) {
        return ret;
    }

    ret = avformat_find_stream_info(ctx->format, nullptr);
    if (ret < 0) {
        return ret;
    }

    ret = av_find_best_stream(ctx->format, type, -1, -1, nullptr, 0);
    if (ret < 0) {
        return ret;
    }
    ctx->stream_index = ret;

    AVStream *stream = ctx->format->streams[ctx->stream_index];
    const AVCodec *decoder = avcodec_find_decoder(stream->codecpar->codec_id);
    if (decoder == nullptr) {
        return AVERROR_DECODER_NOT_FOUND;
    }

    ctx->codec = avcodec_alloc_context3(decoder);
    if (ctx->codec == nullptr) {
        return AVERROR(ENOMEM);
    }

    ret = avcodec_parameters_to_context(ctx->codec, stream->codecpar);
    if (ret < 0) {
        return ret;
    }

    return avcodec_open2(ctx->codec, decoder, nullptr);
}

static void copy_to_window(ANativeWindow_Buffer *window_buffer, const uint8_t *src_data,
                           int src_linesize, int width, int height) {
    auto *dst = static_cast<uint8_t *>(window_buffer->bits);
    int dst_linesize = window_buffer->stride * 4;
    int copy_width = width * 4;
    for (int y = 0; y < height; ++y) {
        std::memcpy(dst + y * dst_linesize, src_data + y * src_linesize, copy_width);
    }
}

static int normalize_rotation(double rotation) {
    if (!std::isfinite(rotation)) {
        return 0;
    }
    int degrees = static_cast<int>(std::lround(rotation)) % 360;
    if (degrees < 0) {
        degrees += 360;
    }
    return (degrees == 90 || degrees == 180 || degrees == 270) ? degrees : 0;
}

static int read_display_rotation(AVStream *stream) {
    if (stream == nullptr || stream->codecpar == nullptr) {
        return 0;
    }
    const AVPacketSideData *side_data = av_packet_side_data_get(
            stream->codecpar->coded_side_data,
            stream->codecpar->nb_coded_side_data,
            AV_PKT_DATA_DISPLAYMATRIX);
    if (side_data == nullptr || side_data->data == nullptr ||
        side_data->size < 9 * sizeof(int32_t)) {
        return 0;
    }
    auto *matrix = reinterpret_cast<const int32_t *>(side_data->data);
    return normalize_rotation(av_display_rotation_get(matrix));
}

static void copy_to_window_with_rotation(ANativeWindow_Buffer *window_buffer,
                                         const uint8_t *src_data,
                                         int src_linesize,
                                         int src_width,
                                         int src_height,
                                         int rotation) {
    if (rotation == 0) {
        copy_to_window(window_buffer, src_data, src_linesize, src_width, src_height);
        return;
    }

    auto *dst = static_cast<uint8_t *>(window_buffer->bits);
    int dst_linesize = window_buffer->stride * 4;
    int dst_width = (rotation == 90 || rotation == 270) ? src_height : src_width;
    int dst_height = (rotation == 90 || rotation == 270) ? src_width : src_height;

    for (int y = 0; y < dst_height; ++y) {
        auto *dst_row = dst + y * dst_linesize;
        for (int x = 0; x < dst_width; ++x) {
            int src_x = x;
            int src_y = y;
            if (rotation == 90) {
                src_x = src_width - 1 - y;
                src_y = x;
            } else if (rotation == 180) {
                src_x = src_width - 1 - x;
                src_y = src_height - 1 - y;
            } else if (rotation == 270) {
                src_x = y;
                src_y = src_height - 1 - x;
            }
            std::memcpy(dst_row + x * 4, src_data + src_y * src_linesize + src_x * 4, 4);
        }
    }
}

/** 读取上层推送的主时钟（音频输出进度），返回 -1 表示主时钟不可用 */
static int64_t master_clock_now(VideoPlayer *player) {
    int64_t anchor_us = player->master_clock_time_us.load();
    if (anchor_us <= 0) {
        return -1;
    }
    int64_t elapsed_us = av_gettime_relative() - anchor_us;
    if (elapsed_us < 0 || elapsed_us > MASTER_CLOCK_TIMEOUT_US) {
        return -1;
    }
    float speed = clamp_speed(player->playback_speed.load());
    return player->master_clock_ms.load() +
           static_cast<int64_t>(elapsed_us * speed / 1000.0);
}

static void rebase_video_clock(VideoPlayer *player, int64_t pts_ms) {
    player->video_clock_pts_ms = pts_ms;
    player->video_clock_time_us = av_gettime_relative();
}

/**
 * 按帧 PTS 与主时钟对齐：需要等待时分片休眠，落后过多时返回 false 要求丢弃该帧。
 * 有音频轨时以音频输出进度为主时钟，否则退化为视频自身时钟（兼容变帧率）。
 */
static bool sync_video_frame(VideoPlayer *player, int64_t pts_ms, int *continuous_drops) {
    if (player->clock_rebase_requested.exchange(false)) {
        rebase_video_clock(player, pts_ms);
        *continuous_drops = 0;
        if (master_clock_now(player) < 0) {
            // 无主时钟：以当前帧重建视频时钟并立即显示
            return true;
        }
    }

    while (!player->stop_requested.load() && !player->pause_requested.load() &&
           player->seek_request_ms.load() < 0) {
        float speed = clamp_speed(player->playback_speed.load());
        int64_t now_us = av_gettime_relative();
        int64_t master = master_clock_now(player);
        int64_t clock;
        if (master >= 0) {
            // 主时钟可用时持续校准视频时钟，主时钟失效后可无缝退化
            clock = master;
            player->video_clock_pts_ms = master;
            player->video_clock_time_us = now_us;
        } else {
            clock = player->video_clock_pts_ms +
                    static_cast<int64_t>((now_us - player->video_clock_time_us) * speed / 1000.0);
            if (std::llabs(pts_ms - clock) > SYNC_RESET_THRESHOLD_MS) {
                // 时间戳跳变或长时间停滞，重建时钟避免整段丢帧
                rebase_video_clock(player, pts_ms);
                clock = pts_ms;
            }
        }

        int64_t diff_ms = pts_ms - clock;
        if (diff_ms < -SYNC_DROP_THRESHOLD_MS) {
            if (*continuous_drops < MAX_CONTINUOUS_DROP_FRAMES) {
                (*continuous_drops)++;
                return false;
            }
            break;
        }
        if (diff_ms <= 0) {
            break;
        }
        // 帧超前时一律继续分片等待：变帧率视频的合法长帧间隔不能提前显示。
        // 仅在主时钟失效（走上面的 SYNC_RESET_THRESHOLD_MS 分支）或音频播完后
        // 由上层清除主时钟时，才会退化为视频自身时钟继续推进。
        int64_t sleep_us = static_cast<int64_t>(diff_ms * 1000.0 / speed);
        if (sleep_us > SYNC_SLEEP_SLICE_US) {
            sleep_us = SYNC_SLEEP_SLICE_US;
        }
        std::this_thread::sleep_for(std::chrono::microseconds(sleep_us));
    }

    *continuous_drops = 0;
    return true;
}

/** 把 RGBA 数据投递到当前窗口，窗口可能已被替换或销毁 */
static void render_video_frame(VideoPlayer *player, const uint8_t *src_data, int src_linesize,
                               int src_width, int src_height, int rotation) {
    std::lock_guard<std::mutex> lock(player->window_mutex);
    ANativeWindow *window = player->window;
    if (window == nullptr) {
        return;
    }
    if (player->window_dirty) {
        ANativeWindow_setBuffersGeometry(window, player->display_width, player->display_height,
                                         WINDOW_FORMAT_RGBA_8888);
        player->window_dirty = false;
    }
    ANativeWindow_Buffer window_buffer;
    if (ANativeWindow_lock(window, &window_buffer, nullptr) != 0) {
        return;
    }
    // 几何尺寸尚未生效时直接跳过，避免越界写入
    if (window_buffer.width >= player->display_width &&
        window_buffer.height >= player->display_height &&
        window_buffer.stride >= player->display_width) {
        copy_to_window_with_rotation(&window_buffer, src_data, src_linesize,
                                     src_width, src_height, rotation);
    }
    ANativeWindow_unlockAndPost(window);
}

static void run_video(VideoPlayer *player) {
    MediaContext media;
    int ret = open_media(player->source, AVMEDIA_TYPE_VIDEO, &media);
    if (ret < 0) {
        player->notifyError(ret, ff_error(ret));
        player->running = false;
        return;
    }
    player->duration_ms = read_duration_ms(media.format, media.stream_index);
    consume_pending_seek(player, &media);

    {
        std::lock_guard<std::mutex> lock(player->window_mutex);
        if (player->window == nullptr) {
            player->notifyError(-1, "Surface is unavailable.");
            player->running = false;
            return;
        }
    }

    AVFrame *frame = av_frame_alloc();
    AVFrame *rgba_frame = av_frame_alloc();
    AVPacket *packet = av_packet_alloc();
    if (frame == nullptr || rgba_frame == nullptr || packet == nullptr) {
        player->notifyError(AVERROR(ENOMEM), "Out of memory.");
        av_frame_free(&frame);
        av_frame_free(&rgba_frame);
        av_packet_free(&packet);
        player->running = false;
        return;
    }

    int width = media.codec->width;
    int height = media.codec->height;
    AVStream *stream = media.format->streams[media.stream_index];
    int rotation = read_display_rotation(stream);
    int display_width = (rotation == 90 || rotation == 270) ? height : width;
    int display_height = (rotation == 90 || rotation == 270) ? width : height;
    std::vector<uint8_t> rgba_buffer(av_image_get_buffer_size(AV_PIX_FMT_RGBA, width, height, 1));
    av_image_fill_arrays(rgba_frame->data, rgba_frame->linesize, rgba_buffer.data(),
                         AV_PIX_FMT_RGBA, width, height, 1);

    SwsContext *sws = sws_getContext(width, height, media.codec->pix_fmt,
                                     width, height, AV_PIX_FMT_RGBA,
                                     SWS_BILINEAR, nullptr, nullptr, nullptr);
    if (sws == nullptr) {
        player->notifyError(-2, "Could not create video scaler.");
        av_frame_free(&frame);
        av_frame_free(&rgba_frame);
        av_packet_free(&packet);
        player->running = false;
        return;
    }

    {
        std::lock_guard<std::mutex> lock(player->window_mutex);
        player->display_width = display_width;
        player->display_height = display_height;
        player->window_dirty = true;
    }
    player->notifyVideoSize(display_width, display_height);

    AVRational frame_rate = av_guess_frame_rate(media.format, stream, nullptr);
    // 帧率仅用于缺失 PTS 时估算时间戳，不再作为播放时钟
    int64_t estimated_frame_ms = 40;
    if (frame_rate.num > 0 && frame_rate.den > 0) {
        estimated_frame_ms = static_cast<int64_t>(1000.0 * frame_rate.den / frame_rate.num);
    }
    if (estimated_frame_ms <= 0) {
        estimated_frame_ms = 1;
    }

    player->notifyPrepared();
    player->notifyProgress(true);

    int continuous_drops = 0;
    int64_t last_pts_ms = -1;
    // 暂停期间发生 seek 时，解码到目标位置附近刷新一帧画面后重新挂起
    bool step_after_seek = false;
    int64_t step_target_ms = 0;
    int step_skipped = 0;

    while (!player->stop_requested.load()) {
        if (!step_after_seek) {
            player->waitForResumeOrSeek();
            if (player->stop_requested.load()) {
                break;
            }
        }
        bool paused_now = player->pause_requested.load();
        if (consume_pending_seek(player, &media)) {
            continuous_drops = 0;
            last_pts_ms = -1;
            if (paused_now) {
                step_after_seek = true;
                step_target_ms = player->current_position_ms.load();
                step_skipped = 0;
            }
        } else if (paused_now && !step_after_seek) {
            continue;
        }

        if (av_read_frame(media.format, packet) < 0) {
            break;
        }
        if (packet->stream_index != media.stream_index) {
            av_packet_unref(packet);
            continue;
        }

        ret = avcodec_send_packet(media.codec, packet);
        av_packet_unref(packet);
        if (ret < 0) {
            continue;
        }

        while (!player->stop_requested.load()) {
            ret = avcodec_receive_frame(media.codec, frame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                break;
            }
            if (ret < 0) {
                player->notifyError(ret, ff_error(ret));
                break;
            }

            int64_t pts_ms = frame_position_ms(frame, stream);
            if (pts_ms <= 0 && last_pts_ms >= 0) {
                // 部分帧缺少 PTS，按估算帧间隔递推，保证时钟单调
                pts_ms = last_pts_ms + estimated_frame_ms;
            }

            bool render;
            if (step_after_seek) {
                // 只需要目标位置附近的一帧，之前的帧全部丢弃（受最大丢帧数保护）
                render = pts_ms >= step_target_ms || step_skipped >= MAX_SEEK_SKIP_FRAMES;
                if (!render) {
                    step_skipped++;
                }
            } else {
                render = sync_video_frame(player, pts_ms, &continuous_drops);
            }

            last_pts_ms = pts_ms;
            if (render) {
                sws_scale(sws, frame->data, frame->linesize, 0, height,
                          rgba_frame->data, rgba_frame->linesize);
                player->current_position_ms = pts_ms;
                player->notifyProgress();
                render_video_frame(player, rgba_frame->data[0], rgba_frame->linesize[0],
                                   width, height, rotation);
            }
            av_frame_unref(frame);

            if (step_after_seek && render) {
                step_after_seek = false;
                rebase_video_clock(player, pts_ms);
                player->clock_rebase_requested = true;
                break;
            }
        }
    }

    sws_freeContext(sws);
    av_frame_free(&frame);
    av_frame_free(&rgba_frame);
    av_packet_free(&packet);

    if (!player->stop_requested.load()) {
        player->notifyCompletion();
    }
    player->running = false;
}

static void run_audio(AudioPlayer *player) {
    MediaContext media;
    int ret = open_media(player->source, AVMEDIA_TYPE_AUDIO, &media);
    if (ret < 0) {
        if (ret == AVERROR_STREAM_NOT_FOUND || ret == AVERROR_DECODER_NOT_FOUND) {
            // 媒体没有可解码的音频轨：先标记不可用，上层据此按视频 PTS 播放
            player->notifyAudioUnavailable();
        }
        player->notifyError(ret, ff_error(ret));
        player->running = false;
        return;
    }
    player->duration_ms = read_duration_ms(media.format, media.stream_index);
    consume_pending_seek(player, &media);

    int out_channels = 2;
    int out_sample_rate = media.codec->sample_rate > 0 ? media.codec->sample_rate : 44100;
    AVChannelLayout out_layout;
    av_channel_layout_default(&out_layout, out_channels);
    if (media.codec->ch_layout.nb_channels <= 0 ||
        media.codec->ch_layout.order == AV_CHANNEL_ORDER_UNSPEC) {
        int in_channels = media.codec->ch_layout.nb_channels > 0
                          ? media.codec->ch_layout.nb_channels
                          : 2;
        av_channel_layout_uninit(&media.codec->ch_layout);
        av_channel_layout_default(&media.codec->ch_layout, in_channels);
    }

    SwrContext *swr = nullptr;
    ret = swr_alloc_set_opts2(&swr,
                              &out_layout,
                              AV_SAMPLE_FMT_S16,
                              out_sample_rate,
                              &media.codec->ch_layout,
                              media.codec->sample_fmt,
                              media.codec->sample_rate,
                              0,
                              nullptr);
    if (ret < 0 || swr == nullptr) {
        player->notifyError(ret, "Could not create audio resampler.");
        av_channel_layout_uninit(&out_layout);
        player->running = false;
        return;
    }

    ret = swr_init(swr);
    if (ret < 0) {
        player->notifyError(ret, ff_error(ret));
        swr_free(&swr);
        av_channel_layout_uninit(&out_layout);
        player->running = false;
        return;
    }

    AVFrame *frame = av_frame_alloc();
    AVPacket *packet = av_packet_alloc();
    if (frame == nullptr || packet == nullptr) {
        player->notifyError(AVERROR(ENOMEM), "Out of memory.");
        av_frame_free(&frame);
        av_packet_free(&packet);
        swr_free(&swr);
        av_channel_layout_uninit(&out_layout);
        player->running = false;
        return;
    }

    player->notifyAudioFormat(out_sample_rate, out_channels);
    player->notifyProgress(true);

    while (!player->stop_requested.load()) {
        player->waitForResumeOrSeek();
        if (player->stop_requested.load()) {
            break;
        }
        consume_pending_seek(player, &media);
        if (player->pause_requested.load()) {
            // 仍处于暂停状态（seek 已消费），回到等待，避免向暂停的 AudioTrack 灌数据
            continue;
        }

        if (av_read_frame(media.format, packet) < 0) {
            break;
        }
        if (packet->stream_index != media.stream_index) {
            av_packet_unref(packet);
            continue;
        }

        ret = avcodec_send_packet(media.codec, packet);
        av_packet_unref(packet);
        if (ret < 0) {
            continue;
        }

        while (!player->stop_requested.load()) {
            ret = avcodec_receive_frame(media.codec, frame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                break;
            }
            if (ret < 0) {
                player->notifyError(ret, ff_error(ret));
                break;
            }
            int64_t frame_pts_ms =
                    frame_position_ms(frame, media.format->streams[media.stream_index]);
            player->current_position_ms = frame_pts_ms;
            player->notifyProgress();

            // 重采样缓存中的样本属于更早的输入，输出块的起始时间需要扣除该延时
            int64_t swr_delay_ms = swr_get_delay(swr, 1000);
            int64_t out_pts_ms = frame_pts_ms - swr_delay_ms;
            if (out_pts_ms < 0) {
                out_pts_ms = 0;
            }

            int dst_samples = av_rescale_rnd(
                    swr_get_delay(swr, media.codec->sample_rate) + frame->nb_samples,
                    out_sample_rate,
                    media.codec->sample_rate,
                    AV_ROUND_UP);
            int buffer_size = av_samples_get_buffer_size(
                    nullptr,
                    out_channels,
                    dst_samples,
                    AV_SAMPLE_FMT_S16,
                    1);
            if (buffer_size <= 0) {
                player->notifyError(buffer_size, "Could not allocate audio output buffer.");
                av_frame_unref(frame);
                continue;
            }
            std::vector<uint8_t> buffer(buffer_size);
            uint8_t *out[] = {buffer.data()};
            int converted = swr_convert(swr, out, dst_samples,
                                        const_cast<const uint8_t **>(frame->extended_data),
                                        frame->nb_samples);
            if (converted > 0) {
                int bytes = converted * out_channels * av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
                player->notifyAudioData(buffer.data(), bytes, out_pts_ms);
            } else if (converted < 0) {
                player->notifyError(converted, ff_error(converted));
            }
            av_frame_unref(frame);
        }
    }

    av_frame_free(&frame);
    av_packet_free(&packet);
    swr_free(&swr);
    av_channel_layout_uninit(&out_layout);

    if (!player->stop_requested.load()) {
        player->notifyCompletion();
    }
    player->running = false;
}

template<typename T>
static void stop_player(T *player) {
    if (player == nullptr) {
        return;
    }
    {
        std::lock_guard<std::mutex> lock(player->mutex);
        player->stop_requested = true;
        player->pause_requested = false;
        player->cv.notify_all();
    }
    if (player->worker.joinable()) {
        player->worker.join();
    }
    player->running = false;
    // 线程已退出，未消费的 seek 不应让上层一直处于「定位中」
    player->seek_request_ms = -1;
    player->seek_done_serial = player->seek_serial.load();
    player->onStopped();
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    g_vm = vm;
    avformat_network_init();
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeCreate(JNIEnv *env, jobject thiz) {
    auto *player = new VideoPlayer();
    player->owner = env->NewGlobalRef(thiz);
    jclass cls = env->GetObjectClass(thiz);
    player->on_prepared = env->GetMethodID(cls, "onNativePrepared", "()V");
    player->on_completion = env->GetMethodID(cls, "onNativeCompletion", "()V");
    player->on_error = env->GetMethodID(cls, "onNativeError", "(ILjava/lang/String;)V");
    player->on_progress = env->GetMethodID(cls, "onNativeProgress", "(JJ)V");
    player->on_video_size = env->GetMethodID(cls, "onNativeVideoSize", "(II)V");
    return reinterpret_cast<jlong>(player);
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeSetDataSource(JNIEnv *env, jclass, jlong handle,
                                                                 jstring source) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    const char *chars = env->GetStringUTFChars(source, nullptr);
    player->source = chars;
    env->ReleaseStringUTFChars(source, chars);
    player->current_position_ms = 0;
    player->duration_ms = 0;
    player->last_progress_callback_ms = -1;
    player->resetSeekState();
    player->onStopped();
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeSetSurface(JNIEnv *env, jclass, jlong handle,
                                                              jobject surface) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    // 播放过程中 Surface 可能被销毁重建，这里直接替换渲染窗口
    std::lock_guard<std::mutex> lock(player->window_mutex);
    if (player->window != nullptr) {
        ANativeWindow_release(player->window);
        player->window = nullptr;
    }
    if (surface != nullptr) {
        player->window = ANativeWindow_fromSurface(env, surface);
        player->window_dirty = true;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeStart(JNIEnv *, jclass, jlong handle) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    if (player->running.exchange(true)) {
        return;
    }
    player->stop_requested = false;
    player->pause_requested = false;
    player->worker = std::thread(run_video, player);
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativePause(JNIEnv *, jclass, jlong handle) {
    reinterpret_cast<VideoPlayer *>(handle)->pause_requested = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeResume(JNIEnv *, jclass, jlong handle) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    std::lock_guard<std::mutex> lock(player->mutex);
    player->pause_requested = false;
    // 暂停期间时钟不推进，恢复时需要重新建立锚点
    player->clock_rebase_requested = true;
    player->cv.notify_all();
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeSetPlaybackSpeed(JNIEnv *, jclass, jlong handle,
                                                                    jfloat speed) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    player->playback_speed = speed > 0.0f ? speed : 1.0f;
    player->clock_rebase_requested = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeSetMasterClock(JNIEnv *, jclass, jlong handle,
                                                                  jlong position_ms) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    if (position_ms < 0) {
        // 负值表示主时钟不再可用（音频已播完或输出停止），立即退化为视频自身时钟。
        // video_clock_* 在主时钟有效期间已被持续校准，因此可无缝衔接继续推进。
        player->master_clock_time_us = 0;
        player->master_clock_ms = 0;
        return;
    }
    player->master_clock_ms = position_ms;
    player->master_clock_time_us = av_gettime_relative();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeIsSeeking(JNIEnv *, jclass, jlong handle) {
    return reinterpret_cast<VideoPlayer *>(handle)->isSeeking()
           ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeGetCurrentPosition(JNIEnv *, jclass,
                                                                      jlong handle) {
    return reinterpret_cast<VideoPlayer *>(handle)->current_position_ms.load();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeGetDuration(JNIEnv *, jclass, jlong handle) {
    return reinterpret_cast<VideoPlayer *>(handle)->duration_ms.load();
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeSeekTo(JNIEnv *, jclass, jlong handle,
                                                          jlong position_ms) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    player->requestSeek(position_ms >= 0 ? position_ms : 0);
    player->notifyProgress(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeStop(JNIEnv *, jclass, jlong handle) {
    stop_player(reinterpret_cast<VideoPlayer *>(handle));
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeRelease(JNIEnv *env, jclass, jlong handle) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    stop_player(player);
    {
        std::lock_guard<std::mutex> lock(player->window_mutex);
        if (player->window != nullptr) {
            ANativeWindow_release(player->window);
            player->window = nullptr;
        }
    }
    env->DeleteGlobalRef(player->owner);
    delete player;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeCreate(JNIEnv *env, jobject thiz) {
    auto *player = new AudioPlayer();
    player->owner = env->NewGlobalRef(thiz);
    jclass cls = env->GetObjectClass(thiz);
    player->on_audio_format = env->GetMethodID(cls, "onNativeAudioFormat", "(II)V");
    player->on_audio_data = env->GetMethodID(cls, "onNativeAudioData", "([BJ)V");
    player->on_audio_flush = env->GetMethodID(cls, "onNativeAudioFlush", "(J)V");
    player->on_audio_unavailable = env->GetMethodID(cls, "onNativeAudioUnavailable", "()V");
    player->on_completion = env->GetMethodID(cls, "onNativeCompletion", "()V");
    player->on_error = env->GetMethodID(cls, "onNativeError", "(ILjava/lang/String;)V");
    player->on_progress = env->GetMethodID(cls, "onNativeProgress", "(JJ)V");
    return reinterpret_cast<jlong>(player);
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeSetDataSource(JNIEnv *env, jclass, jlong handle,
                                                                 jstring source) {
    auto *player = reinterpret_cast<AudioPlayer *>(handle);
    const char *chars = env->GetStringUTFChars(source, nullptr);
    player->source = chars;
    env->ReleaseStringUTFChars(source, chars);
    player->current_position_ms = 0;
    player->duration_ms = 0;
    player->last_progress_callback_ms = -1;
    player->resetSeekState();
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeStart(JNIEnv *, jclass, jlong handle) {
    auto *player = reinterpret_cast<AudioPlayer *>(handle);
    if (player->running.exchange(true)) {
        return;
    }
    player->stop_requested = false;
    player->pause_requested = false;
    player->worker = std::thread(run_audio, player);
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativePause(JNIEnv *, jclass, jlong handle) {
    reinterpret_cast<AudioPlayer *>(handle)->pause_requested = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeResume(JNIEnv *, jclass, jlong handle) {
    auto *player = reinterpret_cast<AudioPlayer *>(handle);
    std::lock_guard<std::mutex> lock(player->mutex);
    player->pause_requested = false;
    player->cv.notify_all();
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeSetPlaybackSpeed(JNIEnv *, jclass, jlong handle,
                                                                    jfloat speed) {
    auto *player = reinterpret_cast<AudioPlayer *>(handle);
    player->playback_speed = speed > 0.0f ? speed : 1.0f;
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeGetCurrentPosition(JNIEnv *, jclass,
                                                                      jlong handle) {
    return reinterpret_cast<AudioPlayer *>(handle)->current_position_ms.load();
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeGetDuration(JNIEnv *, jclass, jlong handle) {
    return reinterpret_cast<AudioPlayer *>(handle)->duration_ms.load();
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeSeekTo(JNIEnv *, jclass, jlong handle,
                                                          jlong position_ms) {
    auto *player = reinterpret_cast<AudioPlayer *>(handle);
    player->requestSeek(position_ms >= 0 ? position_ms : 0);
    player->notifyProgress(true);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeIsSeeking(JNIEnv *, jclass, jlong handle) {
    return reinterpret_cast<AudioPlayer *>(handle)->isSeeking() ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeStop(JNIEnv *, jclass, jlong handle) {
    stop_player(reinterpret_cast<AudioPlayer *>(handle));
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegAudioPlayer_nativeRelease(JNIEnv *env, jclass, jlong handle) {
    auto *player = reinterpret_cast<AudioPlayer *>(handle);
    stop_player(player);
    env->DeleteGlobalRef(player->owner);
    delete player;
}
