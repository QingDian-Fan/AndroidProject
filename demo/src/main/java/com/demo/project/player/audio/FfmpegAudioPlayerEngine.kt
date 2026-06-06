package com.demo.project.player.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.common.player.FfmpegAudioPlayer
import com.common.player.PlayerListener

/**
 * 基于 lib_common-player（FFmpeg + AudioTrack）的音频播放引擎。
 *
 * 用于演示通过 [AudioPlayerEngines] 工厂切换播放内核，与默认的 [ExoAudioPlayerEngine] 等价可替换。
 *
 * 线程模型参考 [com.demo.project.player.CommonPlayerVideoEngine]：所有对 native 播放器的操作都在
 * 独立的 [playerThread] 上串行执行，native 回调统一切回主线程；并自行管理音频焦点。
 */
class FfmpegAudioPlayerEngine(context: Context) : AudioPlayerEngine {

    private companion object {
        private const val TAG = "FfmpegAudioEngine"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val playerThread = HandlerThread("FfmpegAudioPlayerEngine").apply { start() }
    private val playerHandler = Handler(playerThread.looper)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        pause()
                    }
                }
                .build()
        } else {
            null
        }

    @Volatile
    private var released = false

    @Volatile
    private var started = false

    @Volatile
    private var paused = false

    @Volatile
    private var ended = false

    @Volatile
    private var cachedDuration = 0L

    @Volatile
    private var cachedPosition = 0L

    private var needsStopBeforeRestart = false
    private var dataSource: String? = null
    private var playbackSpeed = 1f
    private var listener: AudioPlayerEngine.Listener? = null
    private var audioPlayer: FfmpegAudioPlayer? = null

    private val audioListener = object : PlayerListener {
        override fun onPrepared() {
            cachedDuration = readDuration()
            postToMain {
                if (released) return@postToMain
                started = true
                paused = false
                ended = false
                requestAudioFocus()
                listener?.onReady()
                listener?.onPlayingChanged(true)
            }
        }

        override fun onCompletion() {
            postToMain {
                if (released) return@postToMain
                started = false
                paused = false
                ended = true
                needsStopBeforeRestart = true
                listener?.onEnded()
                listener?.onPlayingChanged(false)
            }
        }

        override fun onProgress(positionMs: Long, durationMs: Long) {
            cachedPosition = positionMs.coerceAtLeast(0L)
            if (durationMs > 0L) {
                cachedDuration = durationMs
            }
        }

        override fun onError(code: Int, message: String?) {
            postError(RuntimeException("FFmpeg audio player error($code): ${message.orEmpty()}"))
        }
    }

    override val isPlaying: Boolean
        get() = started && !paused && !ended

    override val isEnded: Boolean
        get() = ended

    override val duration: Long
        get() = cachedDuration

    override val currentPosition: Long
        get() = cachedPosition

    override val bufferedPosition: Long
        get() = cachedPosition

    override fun setListener(listener: AudioPlayerEngine.Listener?) {
        this.listener = listener
    }

    override fun setDataSource(urlString: String) {
        dataSource = urlString
        started = false
        paused = false
        ended = false
        needsStopBeforeRestart = false
        cachedDuration = 0L
        cachedPosition = 0L
        postPlayerAction {
            stopPlayer()
            ensurePlayer()
            audioPlayer?.setDataSource(urlString)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        postPlayerAction {
            ensurePlayer()
            audioPlayer?.setPlaybackSpeed(speed)
        }
    }

    override fun prepare() {
        // FFmpeg 播放器在 start() 时才真正解码，这里只对外通知缓冲态。
        listener?.onBuffering()
    }

    override fun play() {
        if (released) return
        if (started && !ended) {
            // 暂停后恢复
            paused = false
            postPlayerAction { audioPlayer?.resume() }
            postToMain { listener?.onPlayingChanged(true) }
            return
        }
        // 首次播放或播放结束后重新开始
        started = true
        paused = false
        ended = false
        postPlayerAction {
            if (needsStopBeforeRestart) {
                stopPlayer()
                needsStopBeforeRestart = false
            }
            ensurePlayer()
            configurePlayer()
            requestAudioFocus()
            audioPlayer?.start()
        }
    }

    override fun pause() {
        if (!started || ended) return
        paused = true
        postPlayerAction { audioPlayer?.pause() }
        postToMain { listener?.onPlayingChanged(false) }
    }

    override fun seekTo(positionMs: Long) {
        cachedPosition = positionMs.coerceAtLeast(0L)
        postPlayerAction { audioPlayer?.seekTo(positionMs) }
    }

    override fun release() {
        released = true
        started = false
        paused = false
        ended = false
        listener = null
        playerHandler.post {
            runCatching {
                audioPlayer?.setListener(null)
                audioPlayer?.release()
                audioPlayer = null
            }
            abandonAudioFocus()
            playerThread.quitSafely()
        }
    }

    private fun ensurePlayer() {
        if (released) return
        if (audioPlayer == null) {
            audioPlayer = FfmpegAudioPlayer().also { it.setListener(audioListener) }
        }
    }

    private fun configurePlayer() {
        val source = dataSource ?: return
        audioPlayer?.run {
            setDataSource(source)
            setPlaybackSpeed(playbackSpeed)
        }
    }

    private fun stopPlayer() {
        runCatching { audioPlayer?.stop() }
    }

    private fun readDuration(): Long =
        runCatching { audioPlayer?.duration ?: cachedDuration }
            .getOrDefault(cachedDuration)
            .coerceAtLeast(0L)

    private fun postPlayerAction(action: () -> Unit) {
        if (released) return
        playerHandler.post {
            if (released) return@post
            runCatching(action).onFailure(::postError)
        }
    }

    private fun postError(error: Throwable) {
        Log.e(TAG, "player error", error)
        started = false
        paused = false
        ended = false
        needsStopBeforeRestart = true
        postToMain {
            if (!released) listener?.onError(error)
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::requestAudioFocus)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun postToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
