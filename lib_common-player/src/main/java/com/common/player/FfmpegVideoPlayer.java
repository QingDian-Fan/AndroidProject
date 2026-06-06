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

    private static native void nativeStop(long handle);

    private static native void nativeRelease(long handle);
}
