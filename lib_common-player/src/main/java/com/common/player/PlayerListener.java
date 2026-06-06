package com.common.player;

public interface PlayerListener {
    default void onPrepared() {
    }

    default void onCompletion() {
    }

    default void onProgress(long positionMs, long durationMs) {
    }

    default void onError(int code, String message) {
    }
}
