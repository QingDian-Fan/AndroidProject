package com.common.weight.video

import android.view.SurfaceView

interface VideoPlayerEngine {

    val isPlaying: Boolean

    val isEnded: Boolean

    val duration: Long

    val currentPosition: Long

    val bufferedPosition: Long

    fun attachSurface(surfaceView: SurfaceView)

    fun setListener(listener: Listener?)

    fun setDataSource(urlString: String)

    fun setPlaybackSpeed(speed: Float)

    fun prepare()

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun release()

    interface Listener {

        fun onBuffering()

        fun onReady()

        fun onEnded()

        fun onError(error: Throwable)

        fun onVideoSizeChanged(width: Int, height: Int)

        /**
         * 倍速请求的处理结果。引擎可能异步应用倍速（FFmpeg 需要先确认 AudioTrack 能否接受），
         * 调用方必须以本回调为准更新 UI，不能假设 [setPlaybackSpeed] 立即生效。
         *
         * @param speed   当前**实际生效**的倍速；[applied] 为 false 时是回退后的原倍速
         * @param applied 本次请求是否成功；false 表示音视频已一并保持在原倍速
         */
        fun onPlaybackSpeedChanged(speed: Float, applied: Boolean) {}
    }
}
