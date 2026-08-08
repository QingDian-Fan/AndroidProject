package com.common.player;

import android.view.Surface;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FfmpegVideoPlayer {
    private long nativeHandle;
    private String dataSource;
    private String currentQualityName;
    private Surface surface;
    private PlayerListener listener;
    private final Map<String, String> qualitySources = new LinkedHashMap<>();

    public FfmpegVideoPlayer() {
        FfmpegPlayer.loadLibraries();
        nativeHandle = nativeCreate();
    }

    public void setDataSource(String pathOrUrl) {
        dataSource = pathOrUrl;
        currentQualityName = null;
        nativeSetDataSource(requireHandle(), pathOrUrl);
    }

    public void setQualitySources(Map<String, String> sources) {
        qualitySources.clear();
        if (sources != null) {
            qualitySources.putAll(sources);
        }
    }

    public String getCurrentQualityName() {
        return currentQualityName;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
        nativeSetSurface(requireHandle(), surface);
    }

    public void setListener(PlayerListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (dataSource == null || dataSource.isEmpty()) {
            throw new IllegalStateException("Data source is empty.");
        }
        if (surface == null || !surface.isValid()) {
            throw new IllegalStateException("Surface is null or invalid.");
        }
        nativeStart(requireHandle());
    }

    public void pause() {
        nativePause(requireHandle());
    }

    public void resume() {
        nativeResume(requireHandle());
    }

    public void setPlaybackSpeed(float speed) {
        if (speed <= 0f) {
            throw new IllegalArgumentException("Playback speed must be greater than 0.");
        }
        nativeSetPlaybackSpeed(requireHandle(), speed);
    }

    public long getCurrentPosition() {
        return nativeGetCurrentPosition(requireHandle());
    }

    public long getDuration() {
        return nativeGetDuration(requireHandle());
    }

    public void seekTo(long positionMs) {
        nativeSeekTo(requireHandle(), Math.max(0L, positionMs));
    }

    /**
     * 推送播放主时钟（通常为音频实际输出进度），视频帧按其 PTS 与该时钟对齐显示或丢弃。
     * 超过 1 秒未推送时 native 侧自动退化为视频自身 PTS 时钟。
     */
    public void setMasterClock(long positionMs) {
        nativeSetMasterClock(requireHandle(), Math.max(0L, positionMs));
    }

    /**
     * 清除主时钟，视频改用自身 PTS 时钟继续推进。
     * 音频播放完毕或输出停止后必须调用，否则视频会一直等待不再前进的主时钟。
     */
    public void clearMasterClock() {
        nativeSetMasterClock(requireHandle(), -1L);
    }

    /** 解码器是否仍在处理最近一次 seek 请求 */
    public boolean isSeeking() {
        return nativeHandle != 0 && nativeIsSeeking(nativeHandle);
    }

    /**
     * 视频解码器是否已排空。demux 读到 EOF 不代表解码器排空：
     * B 帧会缓存在解码器内部，必须送空包并取到 EOF 才算真正播完。
     *
     * 这是「视频侧是否播完」的唯一权威来源。
     */
    public boolean isDecoderDrained() {
        return nativeHandle != 0 && nativeIsDecoderDrained(nativeHandle);
    }

    /** Debug 帧调度统计快照，native 未就绪时返回 null */
    public FrameStats getFrameStats() {
        if (nativeHandle == 0) {
            return null;
        }
        long[] values = nativeGetFrameStats(nativeHandle);
        if (values == null || values.length < 5) {
            return null;
        }
        return new FrameStats(values[0], values[1], values[2], values[3], values[4]);
    }

    /** 帧调度统计：用于区分设备性能不足、屏幕刷新限制与调度异常 */
    public static final class FrameStats {
        public final long decodedFrames;
        public final long renderedFrames;
        public final long droppedFrames;
        public final long maxConsecutiveDrops;
        public final long sendPacketEagain;

        FrameStats(long decodedFrames, long renderedFrames, long droppedFrames,
                   long maxConsecutiveDrops, long sendPacketEagain) {
            this.decodedFrames = decodedFrames;
            this.renderedFrames = renderedFrames;
            this.droppedFrames = droppedFrames;
            this.maxConsecutiveDrops = maxConsecutiveDrops;
            this.sendPacketEagain = sendPacketEagain;
        }

        @Override
        public String toString() {
            return "decoded=" + decodedFrames
                    + " rendered=" + renderedFrames
                    + " dropped=" + droppedFrames
                    + " maxConsecutiveDrops=" + maxConsecutiveDrops
                    + " sendEagain=" + sendPacketEagain;
        }
    }

    public void switchQuality(String pathOrUrl) {
        long positionMs = getCurrentPosition();
        stop();
        setDataSource(pathOrUrl);
        seekTo(positionMs);
        start();
    }

    public void switchQualityByName(String qualityName) {
        String pathOrUrl = qualitySources.get(qualityName);
        if (pathOrUrl == null || pathOrUrl.isEmpty()) {
            throw new IllegalArgumentException("Unknown quality name: " + qualityName);
        }
        currentQualityName = qualityName;
        long positionMs = getCurrentPosition();
        stop();
        dataSource = pathOrUrl;
        nativeSetDataSource(requireHandle(), pathOrUrl);
        currentQualityName = qualityName;
        seekTo(positionMs);
        start();
    }

    public void stop() {
        if (nativeHandle != 0) {
            nativeStop(nativeHandle);
        }
    }

    public void release() {
        if (nativeHandle != 0) {
            nativeRelease(nativeHandle);
            nativeHandle = 0;
        }
        surface = null;
        listener = null;
    }

    private long requireHandle() {
        if (nativeHandle == 0) {
            throw new IllegalStateException("Player has been released.");
        }
        return nativeHandle;
    }

    @SuppressWarnings("unused")
    private void onNativePrepared() {
        PlayerListener current = listener;
        if (current != null) {
            current.onPrepared();
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
        PlayerListener current = listener;
        if (current != null) {
            current.onError(code, message);
        }
    }

    @SuppressWarnings("unused")
    private void onNativeProgress(long positionMs, long durationMs) {
        PlayerListener current = listener;
        if (current != null) {
            current.onProgress(positionMs, durationMs);
        }
    }

    @SuppressWarnings("unused")
    private void onNativeVideoSize(int width, int height) {
        PlayerListener current = listener;
        if (current != null) {
            current.onVideoSizeChanged(width, height);
        }
    }

    private native long nativeCreate();

    private static native void nativeSetDataSource(long handle, String pathOrUrl);

    private static native void nativeSetSurface(long handle, Surface surface);

    private static native void nativeStart(long handle);

    private static native void nativePause(long handle);

    private static native void nativeResume(long handle);

    private static native void nativeSetPlaybackSpeed(long handle, float speed);

    private static native long nativeGetCurrentPosition(long handle);

    private static native long nativeGetDuration(long handle);

    private static native void nativeSeekTo(long handle, long positionMs);

    private static native void nativeSetMasterClock(long handle, long positionMs);

    private static native boolean nativeIsSeeking(long handle);

    private static native boolean nativeIsDecoderDrained(long handle);

    private static native long[] nativeGetFrameStats(long handle);

    private static native void nativeStop(long handle);

    private static native void nativeRelease(long handle);
}
