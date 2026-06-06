package com.demo.project.player.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * 基于 Media3 [ExoPlayer] 的音频播放引擎（默认实现）。
 *
 * ExoPlayer 要求在创建它的线程（这里是主线程）调用，所有方法均应在主线程使用。
 * 已开启自动音频焦点管理与「拔出耳机自动暂停」。
 */
class ExoAudioPlayerEngine(context: Context) : AudioPlayerEngine {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        setHandleAudioBecomingNoisy(true)
    }

    private var listener: AudioPlayerEngine.Listener? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> listener?.onBuffering()
                Player.STATE_READY -> listener?.onReady()
                Player.STATE_ENDED -> listener?.onEnded()
                Player.STATE_IDLE -> Unit
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            listener?.onPlayingChanged(isPlaying)
        }

        override fun onPlayerError(error: PlaybackException) {
            listener?.onError(error)
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
        get() = player.duration.let { if (it == C.TIME_UNSET) 0L else it }

    override val currentPosition: Long
        get() = player.currentPosition.coerceAtLeast(0L)

    override val bufferedPosition: Long
        get() = player.bufferedPosition.coerceAtLeast(0L)

    override fun setListener(listener: AudioPlayerEngine.Listener?) {
        this.listener = listener
    }

    override fun setDataSource(urlString: String) {
        player.setMediaItem(MediaItem.fromUri(urlString))
    }

    override fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
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
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun release() {
        listener = null
        player.removeListener(playerListener)
        player.release()
    }
}
