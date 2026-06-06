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
    }
}
