package com.common.player;

final class FfmpegPlayer {
    private static volatile boolean loaded;

    private FfmpegPlayer() {
    }

    static void loadLibraries() {
        if (loaded) {
            return;
        }
        synchronized (FfmpegPlayer.class) {
            if (loaded) {
                return;
            }

            System.loadLibrary("avutil");
            System.loadLibrary("swresample");
            System.loadLibrary("swscale");
            System.loadLibrary("avcodec");
            System.loadLibrary("avformat");
            System.loadLibrary("avfilter");
            System.loadLibrary("avdevice");
            System.loadLibrary("ffmpeg_player");
            loaded = true;
        }
    }
}
