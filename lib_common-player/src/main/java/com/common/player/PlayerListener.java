package com.common.player;

public interface PlayerListener {
    default void onPrepared() {
    }

    default void onCompletion() {
    }

    default void onProgress(long positionMs, long durationMs) {
    }

    /** 视频首帧解码后回调真实宽高（仅视频播放器触发） */
    default void onVideoSizeChanged(int width, int height) {
    }

    default void onError(int code, String message) {
    }
}
