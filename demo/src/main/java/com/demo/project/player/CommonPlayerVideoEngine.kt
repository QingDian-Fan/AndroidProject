package com.demo.project.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.common.player.FfmpegAudioPlayer
import com.common.player.FfmpegVideoPlayer
import com.common.player.PlayerListener
import com.common.weight.video.VideoPlayerEngine

class CommonPlayerVideoEngine(context: Context) : VideoPlayerEngine {

    private companion object {
        private const val TAG = "CommonPlayerVideoEngine"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val playerThread = HandlerThread("CommonPlayerVideoEngine").apply { start() }
    private val playerHandler = Handler(playerThread.looper)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
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
    private var surfaceReady = false

    @Volatile
    private var pendingPlay = false

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

    private var listener: VideoPlayerEngine.Listener? = null
    private var surfaceView: SurfaceView? = null
    private var surfaceCallback: SurfaceHolder.Callback? = null
    private var surface: Surface? = null
    private var dataSource: String? = null
    private var playbackSpeed: Float = 1f
    private var needsStopBeforeRestart = false
    private var videoPlayer: FfmpegVideoPlayer? = null
    private var audioPlayer: FfmpegAudioPlayer? = null

    private val videoListener = object : PlayerListener {
        override fun onPrepared() {
            cachedDuration = readVideoDuration()
            cachedPosition = readVideoPosition()
            postToMain {
                if (released) return@postToMain
                started = true
                paused = false
                ended = false
                listener?.onReady()
            }
        }

        override fun onCompletion() {
            postToMain {
                if (released) return@postToMain
                pendingPlay = false
                started = false
                paused = false
                ended = true
                needsStopBeforeRestart = true
                postPlayerAction { audioPlayer?.stop() }
                listener?.onEnded()
            }
        }

        override fun onProgress(positionMs: Long, durationMs: Long) {
            cachedPosition = positionMs.coerceAtLeast(0L)
            cachedDuration = durationMs.coerceAtLeast(0L)
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            postToMain {
                if (!released) listener?.onVideoSizeChanged(width, height)
            }
        }

        override fun onError(code: Int, message: String?) {
            postPlayerError(RuntimeException("FFmpeg video player error($code): ${message.orEmpty()}"))
        }
    }

    private val audioListener = object : PlayerListener {
        override fun onPrepared() {
            postToMain {
                if (!released && started && !paused) {
                    requestAudioFocus()
                }
            }
        }

        override fun onProgress(positionMs: Long, durationMs: Long) {
            if (cachedPosition <= 0L) {
                cachedPosition = positionMs.coerceAtLeast(0L)
            }
            if (cachedDuration <= 0L) {
                cachedDuration = durationMs.coerceAtLeast(0L)
            }
        }

        override fun onError(code: Int, message: String?) {
            postPlayerError(RuntimeException("FFmpeg audio player error($code): ${message.orEmpty()}"))
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

    override fun attachSurface(surfaceView: SurfaceView) {
        if (this.surfaceView === surfaceView) {
            bindSurfaceIfReady(surfaceView.holder)
            return
        }

        this.surfaceView?.holder?.let { holder ->
            surfaceCallback?.let(holder::removeCallback)
        }

        this.surfaceView = surfaceView
        surfaceCallback = object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                bindSurfaceIfReady(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                bindSurfaceIfReady(holder)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                surfaceReady = false
                surface = null
                postPlayerAction { videoPlayer?.setSurface(null) }
            }
        }.also { callback ->
            surfaceView.holder.addCallback(callback)
        }

        bindSurfaceIfReady(surfaceView.holder)
    }

    override fun setListener(listener: VideoPlayerEngine.Listener?) {
        this.listener = listener
    }

    override fun setDataSource(urlString: String) {
        dataSource = urlString
        pendingPlay = false
        started = false
        paused = false
        ended = false
        needsStopBeforeRestart = false
        cachedDuration = 0L
        cachedPosition = 0L
        postPlayerAction {
            stopPlayers()
            ensurePlayers()
            videoPlayer?.setDataSource(urlString)
            audioPlayer?.setDataSource(urlString)
        }
    }

    override fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed
        postPlayerAction {
            ensurePlayers()
            videoPlayer?.setPlaybackSpeed(speed)
            audioPlayer?.setPlaybackSpeed(speed)
        }
    }

    override fun prepare() {
        listener?.onBuffering()
    }

    override fun play() {
        pendingPlay = true
        if (!surfaceReady) {
            return
        }
        if (started && !ended) {
            paused = false
            postPlayerAction {
                videoPlayer?.resume()
                audioPlayer?.resume()
            }
            return
        }

        pendingPlay = false
        started = true
        paused = false
        ended = false
        postPlayerAction {
            if (needsStopBeforeRestart) {
                stopPlayers()
                needsStopBeforeRestart = false
            }
            ensurePlayers()
            configurePlayers()
            startPlayers()
        }
    }

    override fun pause() {
        pendingPlay = false
        if (!started || ended) {
            return
        }
        paused = true
        postPlayerAction {
            videoPlayer?.pause()
            audioPlayer?.pause()
        }
    }

    override fun seekTo(positionMs: Long) {
        cachedPosition = positionMs.coerceAtLeast(0L)
        postPlayerAction {
            videoPlayer?.seekTo(positionMs)
            audioPlayer?.seekTo(positionMs)
        }
    }

    override fun release() {
        released = true
        pendingPlay = false
        started = false
        paused = false
        ended = false
        surfaceView?.holder?.let { holder ->
            surfaceCallback?.let(holder::removeCallback)
        }
        surfaceView = null
        surfaceCallback = null
        surface = null
        listener = null
        playerHandler.post {
            runCatching {
                videoPlayer?.setListener(null)
                audioPlayer?.setListener(null)
                videoPlayer?.release()
                audioPlayer?.release()
                videoPlayer = null
                audioPlayer = null
            }
            abandonAudioFocus()
            playerThread.quitSafely()
        }
    }

    private fun bindSurfaceIfReady(holder: SurfaceHolder) {
        val holderSurface = holder.surface
        val valid = holderSurface != null && holderSurface.isValid
        surfaceReady = valid
        surface = if (valid) holderSurface else null
        if (!valid) {
            return
        }
        postPlayerAction {
            ensurePlayers()
            videoPlayer?.setSurface(holderSurface)
        }
        if (pendingPlay) {
            play()
        }
    }

    private fun ensurePlayers() {
        if (released) {
            return
        }
        if (videoPlayer == null) {
            videoPlayer = FfmpegVideoPlayer().also { it.setListener(videoListener) }
        }
        if (audioPlayer == null) {
            audioPlayer = FfmpegAudioPlayer().also { it.setListener(audioListener) }
        }
    }

    private fun configurePlayers() {
        val source = dataSource ?: return
        val video = videoPlayer ?: return
        video.setDataSource(source)
        video.setPlaybackSpeed(playbackSpeed)
        surface?.let(video::setSurface)

        audioPlayer?.run {
            setDataSource(source)
            setPlaybackSpeed(playbackSpeed)
        }
    }

    private fun startPlayers() {
        requestAudioFocus()
        videoPlayer?.start()
        audioPlayer?.start()
    }

    private fun stopPlayers() {
        runCatching { videoPlayer?.stop() }
        runCatching { audioPlayer?.stop() }
    }

    private fun readVideoDuration(): Long {
        return runCatching { videoPlayer?.duration ?: cachedDuration }
            .getOrDefault(cachedDuration)
            .coerceAtLeast(0L)
    }

    private fun readVideoPosition(): Long {
        return runCatching { videoPlayer?.currentPosition ?: cachedPosition }
            .getOrDefault(cachedPosition)
            .coerceAtLeast(0L)
    }

    private fun postPlayerAction(action: () -> Unit) {
        if (released) {
            return
        }
        playerHandler.post {
            if (released) {
                return@post
            }
            runCatching(action).onFailure(::postPlayerError)
        }
    }

    private fun postPlayerError(error: Throwable) {
        Log.e(TAG, "player error", error)
        pendingPlay = false
        started = false
        paused = false
        ended = false
        needsStopBeforeRestart = true
        postToMain {
            if (!released) {
                listener?.onError(error)
            }
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
