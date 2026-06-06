package com.common.weight.video

import android.content.Context
import android.view.SurfaceView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

class ExoVideoPlayerEngine(context: Context) : VideoPlayerEngine {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private var listener: VideoPlayerEngine.Listener? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> listener?.onBuffering()
                Player.STATE_READY -> listener?.onReady()
                Player.STATE_ENDED -> listener?.onEnded()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            listener?.onError(error)
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            listener?.onVideoSizeChanged(videoSize.width, videoSize.height)
        }
    }

    init {
        player.addListener(playerListener)
    }

    override val isPlaying: Boolean
        get() = player.isPlaying

    override val isEnded: Boolean
        get() = player.playbackState == Player.STATE_ENDED

    override val duration: Long
        get() = if (player.duration == C.TIME_UNSET) 0L else player.duration

    override val currentPosition: Long
        get() = player.currentPosition

    override val bufferedPosition: Long
        get() = player.bufferedPosition

    override fun attachSurface(surfaceView: SurfaceView) {
        player.setVideoSurfaceView(surfaceView)
    }

    override fun setListener(listener: VideoPlayerEngine.Listener?) {
        this.listener = listener
    }

    override fun setDataSource(urlString: String) {
        player.setMediaItem(MediaItem.fromUri(urlString))
    }

    override fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed)
    }

    override fun prepare() {
        player.prepare()
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun release() {
        player.removeListener(playerListener)
        listener = null
        player.release()
    }
}

object ExoVideoPlayerEngineFactory : VideoPlayerEngineFactory {

    override fun create(context: Context): VideoPlayerEngine {
        return ExoVideoPlayerEngine(context)
    }
}
