package com.demo.project.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
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

        /** 主时钟采样与推送间隔 */
        private const val CLOCK_INTERVAL_MS = 100L

        /** 定位保护超时：超过该时长仍未收到解码器落点，则回到真实时钟 */
        private const val SEEK_TIMEOUT_MS = 5000L

        /** 音频输出停滞多久后判定异常，用于播放结束的兜底（正常推进的音频不受此限制） */
        private const val AUDIO_DRAIN_STALL_TIMEOUT_MS = 3000L
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

    /** 视频解码线程是否已结束 */
    @Volatile
    private var videoDecodeFinished = false

    /** 音频排空检测：最近一次观察到的输出位置及其时间戳 */
    @Volatile
    private var audioDrainPositionMs = -1L

    @Volatile
    private var audioDrainProgressUptimeMs = 0L

    /** 播放结束是否已上报，避免主时钟轮询重复触发 */
    @Volatile
    private var completionNotified = false

    /** 媒体是否存在可解码的音频轨 */
    @Volatile
    private var audioAvailable = true

    /** 拖动保护：>=0 表示正在等待解码器落点，此期间对外只暴露目标位置 */
    @Volatile
    private var seekTargetMs = -1L

    @Volatile
    private var seekStartUptimeMs = 0L

    /** 播放器未运行时的定位目标，下次启动后应用 */
    @Volatile
    private var pendingSeekMs = -1L

    /** 主时钟轮询是否在运行 */
    @Volatile
    private var clockRunning = false

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
            postToMain {
                if (released) return@postToMain
                started = true
                paused = false
                ended = false
                listener?.onReady()
            }
        }

        override fun onCompletion() {
            // 视频解码结束不代表播放结束，还需等待音频输出播完，避免残留声音
            audioDrainPositionMs = -1L
            audioDrainProgressUptimeMs = SystemClock.uptimeMillis()
            videoDecodeFinished = true
            postPlayerAction { finishPlaybackIfDrained() }
        }

        override fun onProgress(positionMs: Long, durationMs: Long) {
            if (durationMs > 0L) {
                cachedDuration = durationMs
            }
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
            if (cachedDuration <= 0L && durationMs > 0L) {
                cachedDuration = durationMs
            }
        }

        override fun onCompletion() {
            postPlayerAction { finishPlaybackIfDrained() }
        }

        override fun onAudioUnavailable() {
            // 无音频轨：改用视频 PTS 作为播放时钟，不影响视频播放
            audioAvailable = false
        }

        override fun onError(code: Int, message: String?) {
            if (!audioAvailable) {
                // 缺少音频轨已单独处理，不作为播放失败上报
                return
            }
            postPlayerError(RuntimeException("FFmpeg audio player error($code): ${message.orEmpty()}"))
        }
    }

    /** 主时钟轮询：统一进度来源，并把音频输出进度推送给视频渲染线程 */
    private val clockRunnable = object : Runnable {
        override fun run() {
            if (released || !clockRunning) {
                return
            }
            runCatching(::updateClock).onFailure { error ->
                com.common.utils.LogUtil.e(TAG, "update clock failed", error)
            }
            if (clockRunning && !released) {
                playerHandler.postDelayed(this, CLOCK_INTERVAL_MS)
            }
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
                // Surface 销毁时音视频成对暂停，等待重建后再恢复，避免后台残留声音
                val resumeAfterRebuild = started && !paused && !ended
                if (resumeAfterRebuild) {
                    paused = true
                    pendingPlay = true
                }
                postPlayerAction {
                    if (resumeAfterRebuild) {
                        videoPlayer?.pause()
                        audioPlayer?.pause()
                    }
                    videoPlayer?.setSurface(null)
                }
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
        resetPlaybackState()
        stopClock()
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
            startClock()
            return
        }

        pendingPlay = false
        started = true
        paused = false
        ended = false
        resetPlaybackState()
        val resumeAt = pendingSeekMs
        pendingSeekMs = -1L
        if (resumeAt > 0L) {
            // 重启后解码器需要重新定位，这段时间内对外保持目标位置
            cachedPosition = resumeAt
            seekTargetMs = resumeAt
            seekStartUptimeMs = SystemClock.uptimeMillis()
        }
        postPlayerAction {
            if (needsStopBeforeRestart) {
                stopPlayers()
                needsStopBeforeRestart = false
            }
            ensurePlayers()
            configurePlayers()
            if (resumeAt > 0L) {
                // setDataSource 会清空 native 的定位请求，重启后需要重新下发
                videoPlayer?.seekTo(resumeAt)
                audioPlayer?.seekTo(resumeAt)
            }
            startPlayers()
        }
        startClock()
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
        val duration = cachedDuration
        var target = positionMs.coerceAtLeast(0L)
        if (duration > 0L) {
            target = target.coerceAtMost(duration)
        }
        cachedPosition = target
        if (ended && (duration <= 0L || target < duration)) {
            // 结束后回拖：重新回到可播放状态，避免被当成重播而丢弃目标位置
            ended = false
        }
        if (started && !ended) {
            seekTargetMs = target
            seekStartUptimeMs = SystemClock.uptimeMillis()
            pendingSeekMs = -1L
        } else {
            // 播放器未运行，记录目标位置，等待下一次 play() 时应用
            seekTargetMs = -1L
            pendingSeekMs = target
        }
        postPlayerAction {
            videoPlayer?.seekTo(target)
            audioPlayer?.seekTo(target)
        }
    }

    override fun release() {
        released = true
        pendingPlay = false
        started = false
        paused = false
        ended = false
        stopClock()
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

    private fun resetPlaybackState() {
        videoDecodeFinished = false
        completionNotified = false
        audioAvailable = true
        seekTargetMs = -1L
        audioDrainPositionMs = -1L
        audioDrainProgressUptimeMs = 0L
    }

    private fun startClock() {
        if (released || clockRunning) {
            return
        }
        clockRunning = true
        playerHandler.removeCallbacks(clockRunnable)
        playerHandler.post(clockRunnable)
    }

    private fun stopClock() {
        clockRunning = false
        playerHandler.removeCallbacks(clockRunnable)
    }

    /**
     * 统一播放时钟：有音频轨时以音频实际输出进度为准，否则使用视频 PTS；
     * 拖动定位未完成前只暴露目标位置，避免进度回跳。
     */
    private fun updateClock() {
        if (released) {
            return
        }
        val video = videoPlayer ?: return
        val audio = audioPlayer
        val hasAudioOutput = audio != null && audioAvailable && audio.isOutputActive

        if (cachedDuration <= 0L) {
            val duration = runCatching { video.duration }.getOrDefault(0L)
            if (duration > 0L) {
                cachedDuration = duration
            }
        }
        val duration = cachedDuration

        val target = seekTargetMs
        if (target >= 0L) {
            val seeking = runCatching { video.isSeeking }.getOrDefault(false) ||
                    (hasAudioOutput && runCatching { audio!!.isSeeking }.getOrDefault(false))
            val expired = SystemClock.uptimeMillis() - seekStartUptimeMs > SEEK_TIMEOUT_MS
            if (seeking && !expired) {
                cachedPosition = clampPosition(target, duration)
                runCatching { video.setMasterClock(cachedPosition) }
                return
            }
            seekTargetMs = -1L
        }

        // 音频播完后其输出位置不再推进，必须撤下主时钟，
        // 否则音轨短于视轨时视频会一直等待一个静止的主时钟
        val audioClockUsable = hasAudioOutput &&
                !runCatching { audio!!.isPlaybackFinished }.getOrDefault(true)

        val position = if (audioClockUsable) {
            runCatching { audio!!.currentPosition }.getOrDefault(cachedPosition)
        } else {
            runCatching { video.currentPosition }.getOrDefault(cachedPosition)
        }
        val next = clampPosition(position, duration)
        // 视频已解码完但音频更长时，时钟从音频切回视频 PTS 会导致进度倒退，尾段保持单调
        cachedPosition = if (videoDecodeFinished) maxOf(next, cachedPosition) else next
        if (audioClockUsable) {
            runCatching { video.setMasterClock(cachedPosition) }
        } else if (hasAudioOutput) {
            runCatching { video.clearMasterClock() }
        }
        finishPlaybackIfDrained()
    }

    private fun clampPosition(position: Long, duration: Long): Long {
        val safePosition = position.coerceAtLeast(0L)
        return if (duration > 0L) safePosition.coerceAtMost(duration) else safePosition
    }

    /**
     * 判断音频输出是否已停滞。只有确认输出长时间没有任何推进（设备异常、写入失败等）
     * 才允许超时兜底结束播放，正常推进的音频必须等到实际播完。
     */
    private fun isAudioOutputStalled(audio: FfmpegAudioPlayer?): Boolean {
        if (audio == null) {
            return true
        }
        val position = runCatching { audio.outputPositionMs }.getOrDefault(-1L)
        val now = SystemClock.uptimeMillis()
        if (position < 0L) {
            // 输出位置不可读，无法确认是否推进，沿用上一次的停滞计时
            if (audioDrainProgressUptimeMs <= 0L) {
                audioDrainProgressUptimeMs = now
            }
        } else if (position > audioDrainPositionMs) {
            audioDrainPositionMs = position
            audioDrainProgressUptimeMs = now
        } else if (audioDrainProgressUptimeMs <= 0L) {
            audioDrainProgressUptimeMs = now
        }
        return now - audioDrainProgressUptimeMs > AUDIO_DRAIN_STALL_TIMEOUT_MS
    }

    /** 视频解码结束且音频已播完时才判定播放完成 */
    private fun finishPlaybackIfDrained() {
        if (released || completionNotified || !videoDecodeFinished) {
            return
        }
        val audio = audioPlayer
        val audioDrained = audio == null || !audioAvailable || !audio.isOutputActive ||
                runCatching { audio.isPlaybackFinished }.getOrDefault(true)
        if (!audioDrained && !isAudioOutputStalled(audio)) {
            // 音频输出仍在推进（音轨可能长于视频轨），等待其真正播完，不截断尾音
            return
        }
        completionNotified = true
        clockRunning = false
        playerHandler.removeCallbacks(clockRunnable)
        if (cachedDuration > 0L) {
            cachedPosition = cachedDuration
        }
        seekTargetMs = -1L
        pendingSeekMs = -1L
        stopPlayers()
        abandonAudioFocus()
        postToMain {
            if (released) return@postToMain
            pendingPlay = false
            started = false
            paused = false
            ended = true
            needsStopBeforeRestart = true
            listener?.onEnded()
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
        com.common.utils.LogUtil.e(TAG, "player error", error)
        pendingPlay = false
        started = false
        paused = false
        ended = false
        needsStopBeforeRestart = true
        seekTargetMs = -1L
        stopClock()
        // 播放失败时成对停止音视频，避免后台残留声音
        playerHandler.post {
            runCatching { stopPlayers() }
            abandonAudioFocus()
        }
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
