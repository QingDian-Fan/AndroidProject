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

    private long nativeHandle;
    private String dataSource;
    private PlayerListener listener;
    private volatile AudioTrack audioTrack;
    private float playbackSpeed = 1.0f;
    private boolean audioErrorNotified;

    /** 停止中标记：用于打断写入循环，避免 native 线程 join 时死锁 */
    private volatile boolean stopping;
    /** native 解码线程是否已结束 */
    private volatile boolean decodeFinished;
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
        decodeFinished = false;
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
        decodeFinished = false;
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

    public void setPlaybackSpeed(float speed) {
        if (speed <= 0f) {
            throw new IllegalArgumentException("Playback speed must be greater than 0.");
        }
        playbackSpeed = speed;
        applyPlaybackSpeed();
        nativeSetPlaybackSpeed(requireHandle(), speed);
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

    /** 解码结束且 AudioTrack 中的数据已全部播放完毕 */
    public boolean isPlaybackFinished() {
        if (!decodeFinished) {
            return false;
        }
        AudioTrack track = audioTrack;
        if (track == null) {
            return true;
        }
        synchronized (clockLock) {
            long head = readHeadPositionLocked(track);
            return head < 0L || head >= writtenFrames;
        }
    }

    public long getDuration() {
        return nativeGetDuration(requireHandle());
    }

    public void seekTo(long positionMs) {
        long target = Math.max(0L, positionMs);
        decodeFinished = false;
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
        int bufferSize = Math.max(minBufferSize, sampleRate * channels * 2 / 2);

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
            setTrackVolume(audioTrack);
            audioTrack.play();
        } catch (RuntimeException e) {
            notifyAudioError(-1004, "Start AudioTrack failed: " + e.getMessage());
            releaseAudioTrack();
            return;
        }
        applyPlaybackSpeed();

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
        decodeFinished = true;
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

    private void applyPlaybackSpeed() {
        AudioTrack track = audioTrack;
        if (track == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        try {
            PlaybackParams params = track.getPlaybackParams();
            params.setSpeed(playbackSpeed);
            track.setPlaybackParams(params);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
        }
    }

    private void setTrackVolume(AudioTrack track) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                track.setVolume(AudioTrack.getMaxVolume());
            } else {
                //noinspection deprecation
                track.setStereoVolume(AudioTrack.getMaxVolume(), AudioTrack.getMaxVolume());
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

    private static native void nativeStop(long handle);

    private static native void nativeRelease(long handle);
}
