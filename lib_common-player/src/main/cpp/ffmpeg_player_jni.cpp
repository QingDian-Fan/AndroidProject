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
#include <libavutil/error.h>
#include <libavutil/imgutils.h>
#include <libavutil/opt.h>
#include <libavutil/time.h>
#include <libswresample/swresample.h>
#include <libswscale/swscale.h>
}

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "FfmpegPlayer", __VA_ARGS__)

static JavaVM *g_vm = nullptr;

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

    virtual ~BasePlayer() = default;

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

    void waitIfPaused() {
        std::unique_lock<std::mutex> lock(mutex);
        cv.wait(lock, [&] {
            return stop_requested.load() || !pause_requested.load();
        });
    }
};

struct VideoPlayer : BasePlayer {
    jobject surface = nullptr;
    jmethodID on_prepared = nullptr;

    void notifyPrepared() {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_prepared != nullptr) {
            env->CallVoidMethod(owner, on_prepared);
        }
        detach_env(attached);
    }
};

struct AudioPlayer : BasePlayer {
    jmethodID on_audio_format = nullptr;
    jmethodID on_audio_data = nullptr;

    void notifyAudioFormat(int sample_rate, int channels) {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_audio_format != nullptr) {
            env->CallVoidMethod(owner, on_audio_format, sample_rate, channels);
        }
        detach_env(attached);
    }

    void notifyAudioData(const uint8_t *data, int size) {
        bool attached = false;
        JNIEnv *env = attach_env(&attached);
        if (env != nullptr && owner != nullptr && on_audio_data != nullptr && size > 0) {
            jbyteArray array = env->NewByteArray(size);
            env->SetByteArrayRegion(array, 0, size, reinterpret_cast<const jbyte *>(data));
            env->CallVoidMethod(owner, on_audio_data, array);
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

static void consume_pending_seek(BasePlayer *player, MediaContext *media) {
    int64_t requested = player->seek_request_ms.exchange(-1);
    if (requested >= 0 && perform_seek(media, requested)) {
        player->current_position_ms = requested;
        player->notifyProgress(true);
    }
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

    bool attached = false;
    JNIEnv *env = attach_env(&attached);
    ANativeWindow *window = nullptr;
    if (env != nullptr && player->surface != nullptr) {
        window = ANativeWindow_fromSurface(env, player->surface);
    }
    detach_env(attached);

    if (window == nullptr) {
        player->notifyError(-1, "Surface is null.");
        player->running = false;
        return;
    }

    AVFrame *frame = av_frame_alloc();
    AVFrame *rgba_frame = av_frame_alloc();
    AVPacket *packet = av_packet_alloc();
    if (frame == nullptr || rgba_frame == nullptr || packet == nullptr) {
        player->notifyError(AVERROR(ENOMEM), "Out of memory.");
        av_frame_free(&frame);
        av_frame_free(&rgba_frame);
        av_packet_free(&packet);
        ANativeWindow_release(window);
        player->running = false;
        return;
    }

    int width = media.codec->width;
    int height = media.codec->height;
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
        ANativeWindow_release(window);
        player->running = false;
        return;
    }

    ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);

    AVStream *stream = media.format->streams[media.stream_index];
    AVRational frame_rate = av_guess_frame_rate(media.format, stream, nullptr);
    int64_t frame_delay_us = 40000;
    if (frame_rate.num > 0 && frame_rate.den > 0) {
        frame_delay_us = static_cast<int64_t>(1000000.0 * frame_rate.den / frame_rate.num);
    }

    player->notifyPrepared();
    player->notifyProgress(true);

    while (!player->stop_requested.load() && av_read_frame(media.format, packet) >= 0) {
        player->waitIfPaused();
        consume_pending_seek(player, &media);
        if (player->stop_requested.load()) {
            av_packet_unref(packet);
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

            sws_scale(sws, frame->data, frame->linesize, 0, height,
                      rgba_frame->data, rgba_frame->linesize);
            player->current_position_ms = frame_position_ms(frame, stream);
            player->notifyProgress();

            ANativeWindow_Buffer window_buffer;
            if (ANativeWindow_lock(window, &window_buffer, nullptr) == 0) {
                copy_to_window(&window_buffer, rgba_frame->data[0], rgba_frame->linesize[0],
                               width, height);
                ANativeWindow_unlockAndPost(window);
            }
            av_frame_unref(frame);
            float speed = player->playback_speed.load();
            if (speed <= 0.0f) {
                speed = 1.0f;
            }
            auto delay = static_cast<int64_t>(frame_delay_us / speed);
            if (delay > 0) {
                std::this_thread::sleep_for(std::chrono::microseconds(delay));
            }
        }
    }

    sws_freeContext(sws);
    av_frame_free(&frame);
    av_frame_free(&rgba_frame);
    av_packet_free(&packet);
    ANativeWindow_release(window);

    if (!player->stop_requested.load()) {
        player->notifyCompletion();
    }
    player->running = false;
}

static void run_audio(AudioPlayer *player) {
    MediaContext media;
    int ret = open_media(player->source, AVMEDIA_TYPE_AUDIO, &media);
    if (ret < 0) {
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

    while (!player->stop_requested.load() && av_read_frame(media.format, packet) >= 0) {
        player->waitIfPaused();
        consume_pending_seek(player, &media);
        if (player->stop_requested.load()) {
            av_packet_unref(packet);
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
            player->current_position_ms = frame_position_ms(frame, media.format->streams[media.stream_index]);
            player->notifyProgress();

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
                player->notifyAudioData(buffer.data(), bytes);
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
    player->stop_requested = true;
    player->pause_requested = false;
    player->cv.notify_all();
    if (player->worker.joinable()) {
        player->worker.join();
    }
    player->running = false;
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
    player->seek_request_ms = -1;
    player->last_progress_callback_ms = -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeSetSurface(JNIEnv *env, jclass, jlong handle,
                                                              jobject surface) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    if (player->surface != nullptr) {
        env->DeleteGlobalRef(player->surface);
        player->surface = nullptr;
    }
    if (surface != nullptr) {
        player->surface = env->NewGlobalRef(surface);
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
    player->pause_requested = false;
    player->cv.notify_all();
}

extern "C" JNIEXPORT void JNICALL
Java_com_common_player_FfmpegVideoPlayer_nativeSetPlaybackSpeed(JNIEnv *, jclass, jlong handle,
                                                                    jfloat speed) {
    auto *player = reinterpret_cast<VideoPlayer *>(handle);
    player->playback_speed = speed > 0.0f ? speed : 1.0f;
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
    player->seek_request_ms = position_ms >= 0 ? position_ms : 0;
    player->current_position_ms = position_ms >= 0 ? position_ms : 0;
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
    if (player->surface != nullptr) {
        env->DeleteGlobalRef(player->surface);
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
    player->on_audio_data = env->GetMethodID(cls, "onNativeAudioData", "([B)V");
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
    player->seek_request_ms = -1;
    player->last_progress_callback_ms = -1;
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
    player->seek_request_ms = position_ms >= 0 ? position_ms : 0;
    player->current_position_ms = position_ms >= 0 ? position_ms : 0;
    player->notifyProgress(true);
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
