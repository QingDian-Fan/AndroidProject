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

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // 重新缓冲、内部停止等都会让播放停下来，统一上报，
            // 便于上层清理依赖“正在播放”的临时状态（长按临时倍速等）
            if (!isPlaying) {
                listener?.onPlaybackSuspended()
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

    /**
     * ExoPlayer 的 Sonic 音频处理器可覆盖 0.75x~3.0x 的全部档位。非法值（<=0）会被
     * [PlaybackParameters] 拒绝并抛出异常，这里统一捕获后以实际生效倍速回调，
     * 避免调用方把失败的请求当成已生效。
     */
    override fun setPlaybackSpeed(speed: Float) {
        val applied = runCatching { player.playbackParameters = PlaybackParameters(speed) }
            .isSuccess
        listener?.onPlaybackSpeedChanged(player.playbackParameters.speed, applied)
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
