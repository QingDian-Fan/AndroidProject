package com.common.player;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.PlaybackParams;
import android.media.AudioTrack;
import android.os.Build;

public final class FfmpegAudioPlayer {
    private static final String TAG = "FfmpegAudioPlayer";

    /** AudioTrack 缓冲区写满时的重试间隔（毫秒） */
    private static final long WRITE_RETRY_INTERVAL_MS = 10L;

    /**
     * 缓冲区按该倍速预留，保证 0.5x~3.0x 倍速切换不会因缓冲区不足失败。
     *
     * 流式 AudioTrack 在 speed 倍速下消耗客户端数据的速度也是 speed 倍，
     * {@link AudioTrack#setPlaybackParams} 会在缓冲区不足以支撑目标倍速时抛异常。
     * 因此该常量必须覆盖 UI 允许选择的最高档位（含长按临时 3.0x）。
     */
    private static final float MAX_SUPPORTED_PLAYBACK_SPEED = 3.0f;

    /** 缓冲区期望容纳的墙钟时长（毫秒） */
    private static final long BUFFER_DURATION_MS = 500L;

    private long nativeHandle;
    private String dataSource;
    private PlayerListener listener;
    private volatile AudioTrack audioTrack;
    private float playbackSpeed = 1.0f;
    /** 输出音量比例，音频焦点 ducking 期间会被压低 */
    private volatile float outputVolume = 1.0f;
    private boolean audioErrorNotified;

    /** 停止中标记：用于打断写入循环，避免 native 线程 join 时死锁 */
    private volatile boolean stopping;
    /** 是否存在可解码的音频轨 */
    private volatile boolean audioAvailable = true;
    /** 是否有 seek 请求尚未被解码线程消费 */
    private volatile boolean awaitingSeekFlush;

    /** 输出时钟状态，由 native 音频线程写入、播放线程读取 */
    private final Object clockLock = new Object();
    private int outputSampleRate;
    private int outputFrameBytes;
    private long anchorPositionMs;
    private long anchorFrame;
    private long writtenFrames;
    private long lastHeadPosition;
    private long headWrapBase;
    private long seekTargetMs = -1L;

    public FfmpegAudioPlayer() {
        FfmpegPlayer.loadLibraries();
        nativeHandle = nativeCreate();
    }

    public void setDataSource(String pathOrUrl) {
        dataSource = pathOrUrl;
        audioErrorNotified = false;
        audioAvailable = true;
        awaitingSeekFlush = false;
        synchronized (clockLock) {
            seekTargetMs = -1L;
            resetOutputClockLocked(0L);
        }
        nativeSetDataSource(requireHandle(), pathOrUrl);
    }

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (dataSource == null || dataSource.isEmpty()) {
            throw new IllegalStateException("Data source is empty.");
        }
        stopping = false;
        nativeStart(requireHandle());
    }

    public void pause() {
        nativePause(requireHandle());
        AudioTrack track = audioTrack;
        if (track != null) {
            try {
                track.pause();
            } catch (IllegalStateException ignored) {
            }
        }
    }

    public void resume() {
        AudioTrack track = audioTrack;
        if (track != null) {
            try {
                track.play();
            } catch (IllegalStateException ignored) {
            }
        }
        nativeResume(requireHandle());
    }

    /**
     * 设置音频倍速。
     *
     * @return 是否切换成功；返回 false 表示 AudioTrack 拒绝该倍速且已回退到原倍速，
     * 调用方必须据此保持视频侧原倍速，否则会造成持续音画不同步
     */
    public boolean setPlaybackSpeed(float speed) {
        if (speed <= 0f) {
            throw new IllegalArgumentException("Playback speed must be greater than 0.");
        }
        float previousSpeed = playbackSpeed;
        playbackSpeed = speed;
        if (!applyPlaybackSpeed()) {
            // AudioTrack 拒绝该倍速（通常是缓冲区不足）。此时必须回退，
            // 否则解码侧按新倍速推进而实际输出仍是旧倍速，会造成持续音画不同步。
            playbackSpeed = previousSpeed;
            applyPlaybackSpeed();
            com.common.utils.LogUtil.e(TAG,
                    "AudioTrack rejected playback speed " + speed + ", keep " + previousSpeed);
            return false;
        }
        nativeSetPlaybackSpeed(requireHandle(), speed);
        return true;
    }

    /** 实际生效的倍速：AudioTrack 拒绝切速时与请求值不同 */
    public float getPlaybackSpeed() {
        return playbackSpeed;
    }

    /**
     * 设置输出音量比例（0~1）。用于音频焦点 {@code AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK}
     * 期间压低音量，恢复焦点后还原为 1。音量在建轨时也会重新下发。
     */
    public void setVolume(float volume) {
        float safeVolume = Math.max(0f, Math.min(volume, 1f));
        outputVolume = safeVolume;
        applyTrackVolume(audioTrack);
    }

    /**
     * 当前播放位置：优先返回 AudioTrack 实际输出进度（可作为音视频同步主时钟），
     * 尚未建立输出时退化为解码位置。
     */
    public long getCurrentPosition() {
        long outputPosition = getOutputPositionMs();
        if (outputPosition >= 0L) {
            return outputPosition;
        }
        return nativeGetCurrentPosition(requireHandle());
    }

    /**
     * AudioTrack 实际输出进度（毫秒），返回负数表示输出尚未建立。
     * 该值基于播放头位置换算，因此与真实听到的声音一致，不受解码缓冲影响。
     */
    public long getOutputPositionMs() {
        AudioTrack track = audioTrack;
        if (track == null) {
            return -1L;
        }
        synchronized (clockLock) {
            if (awaitingSeekFlush && seekTargetMs >= 0L) {
                // 定位尚未生效，先返回目标位置，避免进度回跳
                return seekTargetMs;
            }
            if (outputSampleRate <= 0) {
                return -1L;
            }
            long head = readHeadPositionLocked(track);
            if (head < 0L) {
                return -1L;
            }
            long position = anchorPositionMs + (head - anchorFrame) * 1000L / outputSampleRate;
            return Math.max(0L, position);
        }
    }

    /** 是否已经建立 AudioTrack 输出（即媒体确实带有可播放的音频） */
    public boolean isOutputActive() {
        return audioTrack != null;
    }

    /** 媒体中是否存在可解码的音频轨 */
    public boolean isAudioAvailable() {
        return audioAvailable;
    }

    /** 是否仍在处理最近一次 seek 请求 */
    public boolean isSeeking() {
        if (awaitingSeekFlush) {
            return true;
        }
        return nativeHandle != 0 && nativeIsSeeking(nativeHandle);
    }

    /**
     * 音频解码器与 SWR 是否均已排空。
     *
     * 这是「音频解码侧是否播完」的唯一权威来源，由 native 解码层提供；
     * 与 [isOutputDrained] 分开表达——解码排空不代表 AudioTrack 已经把 PCM 播出去。
     */
    public boolean isDecoderDrained() {
        return nativeHandle != 0 && nativeIsDecoderDrained(nativeHandle);
    }

    /**
     * AudioTrack 是否已播放完全部写入的 PCM。
     *
     * 依据「播放头位置 >= 已写入帧数」判断，而不是 native 音频线程是否结束，
     * 因此不会截断仍在缓冲区里的尾音。这是音频输出层的唯一权威来源。
     */
    public boolean isOutputDrained() {
        AudioTrack track = audioTrack;
        if (track == null) {
            // 没有建立输出（无音轨或建轨失败）时视为无需等待
            return true;
        }
        synchronized (clockLock) {
            long head = readHeadPositionLocked(track);
            return head < 0L || head >= writtenFrames;
        }
    }

    /** 已写入 AudioTrack 的帧数快照，仅用于 Debug 统计 */
    public long getWrittenFrames() {
        synchronized (clockLock) {
            return writtenFrames;
        }
    }

    /** AudioTrack 播放头帧数快照，仅用于 Debug 统计；返回负数表示不可读 */
    public long getPlayedFrames() {
        AudioTrack track = audioTrack;
        if (track == null) {
            return -1L;
        }
        synchronized (clockLock) {
            return readHeadPositionLocked(track);
        }
    }

    public long getDuration() {
        return nativeGetDuration(requireHandle());
    }

    public void seekTo(long positionMs) {
        long target = Math.max(0L, positionMs);
        synchronized (clockLock) {
            seekTargetMs = target;
        }
        awaitingSeekFlush = true;
        nativeSeekTo(requireHandle(), target);
    }

    public void stop() {
        stopping = true;
        if (nativeHandle != 0) {
            nativeStop(nativeHandle);
        }
        releaseAudioTrack();
        awaitingSeekFlush = false;
        synchronized (clockLock) {
            seekTargetMs = -1L;
            resetOutputClockLocked(0L);
        }
    }

    public void release() {
        stopping = true;
        if (nativeHandle != 0) {
            nativeRelease(nativeHandle);
            nativeHandle = 0;
        }
        releaseAudioTrack();
        listener = null;
    }

    private long requireHandle() {
        if (nativeHandle == 0) {
            throw new IllegalStateException("Player has been released.");
        }
        return nativeHandle;
    }

    @SuppressWarnings("unused")
    private void onNativeAudioFormat(int sampleRate, int channels) {
        releaseAudioTrack();
        int channelMask = channels == 1
                ? AudioFormat.CHANNEL_OUT_MONO
                : AudioFormat.CHANNEL_OUT_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                channelMask,
                AudioFormat.ENCODING_PCM_16BIT
        );
        if (minBufferSize <= 0) {
            notifyAudioError(-1001, "Invalid AudioTrack buffer size: " + minBufferSize);
            return;
        }
        int bufferSize = calculateBufferSize(minBufferSize, sampleRate, channels);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioTrack = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setSampleRate(sampleRate)
                                .setChannelMask(channelMask)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .build())
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(bufferSize)
                        .build();
            } else {
                audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        channelMask,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                );
            }
        } catch (RuntimeException e) {
            notifyAudioError(-1002, "Create AudioTrack failed: " + e.getMessage());
            return;
        }
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            notifyAudioError(-1003, "AudioTrack is not initialized.");
            releaseAudioTrack();
            return;
        }
        try {
            applyTrackVolume(audioTrack);
            audioTrack.play();
        } catch (RuntimeException e) {
            notifyAudioError(-1004, "Start AudioTrack failed: " + e.getMessage());
            releaseAudioTrack();
            return;
        }
        if (!applyPlaybackSpeed()) {
            // 音频无法按目标倍速播放，而 native 视频已按该倍速渲染，继续播放必然音画不同步
            notifyAudioError(-1005, "Apply audio playback speed failed: " + playbackSpeed);
            releaseAudioTrack();
            return;
        }

        synchronized (clockLock) {
            outputSampleRate = sampleRate;
            outputFrameBytes = channels * 2;
            // 新建的 AudioTrack 播放头从 0 开始，锚点位置保留（可能来自 seek）
            resetTrackFramesLocked();
        }

        PlayerListener current = listener;
        if (current != null) {
            current.onPrepared();
        }
    }

    @SuppressWarnings("unused")
    private void onNativeAudioData(byte[] pcm, long ptsMs) {
        if (pcm.length == 0) {
            return;
        }
        synchronized (clockLock) {
            if (outputFrameBytes <= 0) {
                return;
            }
            // 以本块 PCM 的起始时间作为输出时钟锚点，写入前记录，保证播放头换算正确
            anchorPositionMs = Math.max(0L, ptsMs);
            anchorFrame = writtenFrames;
        }

        int offset = 0;
        while (offset < pcm.length && !stopping && !awaitingSeekFlush) {
            AudioTrack track = audioTrack;
            if (track == null) {
                return;
            }
            int written;
            try {
                written = track.write(pcm, offset, pcm.length - offset, AudioTrack.WRITE_NON_BLOCKING);
            } catch (IllegalStateException e) {
                return;
            }
            if (written < 0) {
                notifyAudioError(written, "AudioTrack write failed.");
                return;
            }
            if (written == 0) {
                // 缓冲区已满或处于暂停状态，稍后重试，保持解码节奏与播放一致
                try {
                    Thread.sleep(WRITE_RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                continue;
            }
            offset += written;
            synchronized (clockLock) {
                if (outputFrameBytes > 0) {
                    writtenFrames += written / outputFrameBytes;
                }
            }
        }
    }

    /** 解码器完成定位：丢弃 AudioTrack 中的旧数据并把输出时钟重置到落点 */
    @SuppressWarnings("unused")
    private void onNativeAudioFlush(long positionMs) {
        AudioTrack track = audioTrack;
        if (track != null) {
            try {
                boolean playing = track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
                track.pause();
                track.flush();
                if (playing) {
                    track.play();
                }
            } catch (IllegalStateException ignored) {
            }
        }
        synchronized (clockLock) {
            resetOutputClockLocked(Math.max(0L, positionMs));
            seekTargetMs = -1L;
        }
        awaitingSeekFlush = false;
    }

    @SuppressWarnings("unused")
    private void onNativeAudioUnavailable() {
        audioAvailable = false;
        PlayerListener current = listener;
        if (current != null) {
            current.onAudioUnavailable();
        }
    }

    @SuppressWarnings("unused")
    private void onNativeCompletion() {
        PlayerListener current = listener;
        if (current != null) {
            current.onCompletion();
        }
    }

    @SuppressWarnings("unused")
    private void onNativeError(int code, String message) {
        notifyAudioError(code, message);
    }

    @SuppressWarnings("unused")
    private void onNativeProgress(long positionMs, long durationMs) {
        PlayerListener current = listener;
        if (current != null) {
            current.onProgress(positionMs, durationMs);
        }
    }

    /**
     * 把当前倍速下发给 AudioTrack。
     *
     * @return 是否已按目标倍速生效；输出尚未建立时视为成功（建轨时会重新下发）
     */
    private boolean applyPlaybackSpeed() {
        AudioTrack track = audioTrack;
        if (track == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        try {
            PlaybackParams params = track.getPlaybackParams();
            params.setSpeed(playbackSpeed);
            track.setPlaybackParams(params);
            return true;
        } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException e) {
            com.common.utils.LogUtil.e(TAG,
                    "setPlaybackParams(" + playbackSpeed + ") failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 计算 AudioTrack 缓冲区大小。
     *
     * 流式 AudioTrack 在倍速播放时消耗客户端数据的速度是 speed 倍，缓冲区必须按最大支持倍速放大，
     * 否则 {@link AudioTrack#setPlaybackParams} 会因缓冲区不足抛异常导致无法切速。
     */
    private int calculateBufferSize(int minBufferSize, int sampleRate, int channels) {
        float speed = Math.max(MAX_SUPPORTED_PLAYBACK_SPEED, playbackSpeed);
        int frameBytes = channels * 2;
        // 下限：倍速放大后的系统最小缓冲区
        long required = (long) Math.ceil((double) minBufferSize * speed);
        // 期望：按目标倍速仍能缓冲 BUFFER_DURATION_MS 的墙钟时长
        long preferred = (long) Math.ceil(
                (double) sampleRate * frameBytes * speed * BUFFER_DURATION_MS / 1000.0);
        long bufferSize = Math.max(required, preferred);
        // 对齐到帧边界，避免出现半帧数据
        bufferSize = (bufferSize + frameBytes - 1) / frameBytes * frameBytes;
        return (int) Math.min(bufferSize, Integer.MAX_VALUE - frameBytes);
    }

    private void applyTrackVolume(AudioTrack track) {
        if (track == null) {
            return;
        }
        float volume = AudioTrack.getMaxVolume() * outputVolume;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(volume);
            } else {
                //noinspection deprecation
                track.setStereoVolume(volume, volume);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private void notifyAudioError(int code, String message) {
        String safeMessage = message == null ? "" : message;
        com.common.utils.LogUtil.e(TAG, "audio error " + code + ": " + safeMessage);
        if (audioErrorNotified) {
            return;
        }
        audioErrorNotified = true;
        PlayerListener current = listener;
        if (current != null) {
            current.onError(code, safeMessage);
        }
    }

    private void releaseAudioTrack() {
        AudioTrack track = audioTrack;
        audioTrack = null;
        synchronized (clockLock) {
            outputSampleRate = 0;
            outputFrameBytes = 0;
            resetTrackFramesLocked();
        }
        if (track != null) {
            try {
                track.stop();
            } catch (IllegalStateException ignored) {
            }
            track.release();
        }
    }

    /** 把输出时钟锚点重置到指定位置（新建输出或定位完成时调用） */
    private void resetOutputClockLocked(long positionMs) {
        anchorPositionMs = positionMs;
        resetTrackFramesLocked();
    }

    /** AudioTrack 被新建或 flush 后播放头归零，帧计数需要同步复位 */
    private void resetTrackFramesLocked() {
        anchorFrame = 0L;
        writtenFrames = 0L;
        lastHeadPosition = 0L;
        headWrapBase = 0L;
    }

    /** 读取播放头位置并处理 32 位回绕，返回负数表示读取失败 */
    private long readHeadPositionLocked(AudioTrack track) {
        long head;
        try {
            head = (track.getPlaybackHeadPosition() & 0xFFFFFFFFL) + headWrapBase;
        } catch (IllegalStateException e) {
            return -1L;
        }
        if (head < lastHeadPosition) {
            headWrapBase += 1L << 32;
            head += 1L << 32;
        }
        lastHeadPosition = head;
        return head;
    }

    private native long nativeCreate();

    private static native void nativeSetDataSource(long handle, String pathOrUrl);

    private static native void nativeStart(long handle);

    private static native void nativePause(long handle);

    private static native void nativeResume(long handle);

    private static native void nativeSetPlaybackSpeed(long handle, float speed);

    private static native long nativeGetCurrentPosition(long handle);

    private static native long nativeGetDuration(long handle);

    private static native void nativeSeekTo(long handle, long positionMs);

    private static native boolean nativeIsSeeking(long handle);

    private static native boolean nativeIsDecoderDrained(long handle);

    private static native void nativeStop(long handle);

    private static native void nativeRelease(long handle);
}
