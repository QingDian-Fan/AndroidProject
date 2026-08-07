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
import com.common.weight.video.VideoPausedReason
import com.common.weight.video.allowsFocusGainResume
import com.common.weight.video.clearsPlayIntent
import com.common.weight.video.VideoPlaybackException
import com.common.weight.video.VideoPlayerEngine
import com.common.weight.video.VideoPlayerErrorType

class CommonPlayerVideoEngine(context: Context) : VideoPlayerEngine {

    private companion object {
        private const val TAG = "CommonPlayerVideoEngine"

        /** 主时钟采样与推送间隔 */
        private const val CLOCK_INTERVAL_MS = 100L

        /** 定位保护超时：超过该时长仍未收到解码器落点，则回到真实时钟 */
        private const val SEEK_TIMEOUT_MS = 5000L

        /** 音频输出停滞多久后判定异常，用于播放结束的兜底（正常推进的音频不受此限制） */
        private const val AUDIO_DRAIN_STALL_TIMEOUT_MS = 3000L

        /** 短暂失去音频焦点且允许降低音量时使用的输出音量比例 */
        private const val DUCK_VOLUME = 0.2f

        // 与 ffmpeg_player_jni.cpp / FfmpegAudioPlayer 约定的错误码，用于区分错误类型
        /** native 侧 Surface 不可用 */
        private const val NATIVE_ERROR_SURFACE_UNAVAILABLE = -1

        /** 网络打开、探测或读取被中断/超时 */
        private const val NATIVE_ERROR_IO_ABORTED = -1100

        /** 数据源打开失败 */
        private const val NATIVE_ERROR_OPEN_FAILED = -1101

        /** 播放中读取失败：I/O 错误、连接重置、服务端断开、重连耗尽等 */
        private const val NATIVE_ERROR_READ_FAILED = -1102

        /** 读到无法继续解析的损坏数据 */
        private const val NATIVE_ERROR_READ_INVALID_DATA = -1103

        /** AudioTrack 相关错误码区间（-1005 ~ -1001） */
        private const val NATIVE_ERROR_AUDIO_TRACK_FIRST = -1001
        private const val NATIVE_ERROR_AUDIO_TRACK_LAST = -1005
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val playerThread = HandlerThread("CommonPlayerVideoEngine").apply { start() }
    private val playerHandler = Handler(playerThread.looper)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    /**
     * 音频焦点变化统一处理。
     * 永久丢失：暂停并放弃焦点，不自动恢复；
     * 临时丢失：暂停并记录，重新获得焦点后仅在用户仍期望播放时恢复；
     * 可降低音量：不暂停，压低 AudioTrack 音量；
     * 重新获得：恢复音量，必要时恢复播放。
     */
    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久丢失：清除续播标记与用户播放意图，后续 Gain / 前后台 / Surface 重建
                // 都不得自动恢复，必须由用户重新点击播放
                pausedByFocusLoss = false
                playIntended = false
                applyAudioDucking(false)
                pause(VideoPausedReason.AUDIO_FOCUS_PERMANENT)
                abandonAudioFocus()
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 只有丢失焦点前用户确实期望播放，才允许在重新获得焦点后续播
                pausedByFocusLoss = playIntended && isPlaying
                pause(VideoPausedReason.AUDIO_FOCUS_TRANSIENT)
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                applyAudioDucking(true)
            }

            AudioManager.AUDIOFOCUS_GAIN -> {
                applyAudioDucking(false)
                // 只有「确因临时焦点丢失而暂停」且用户意图仍然有效时才自动续播；
                // 期间发生的用户暂停、切后台、错误、结束都会把标记清掉
                if (pausedByFocusLoss && playIntended && canResumeFromFocusGain()) {
                    pausedByFocusLoss = false
                    play()
                    postToMain {
                        if (!released) listener?.onPlaybackResumed()
                    }
                } else {
                    pausedByFocusLoss = false
                }
            }
        }
    }

    /**
     * 焦点恢复的前置条件：播放器未释放、未结束、未处于错误停止状态，
     * 且 Surface 可用或引擎可以安全等待 Surface（[play] 会记录 pendingPlay）。
     */
    private fun canResumeFromFocusGain(): Boolean = !released && !ended && started

    private val audioFocusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusListener)
                .build()
        } else {
            null
        }

    /** 是否因临时失去音频焦点而暂停：重新获得焦点后据此决定是否续播 */
    @Volatile
    private var pausedByFocusLoss = false

    /**
     * 用户播放意图。独立于瞬时播放状态：准备中、缓冲中、等待 Surface 时 [isPlaying] 为 false，
     * 但意图仍为 true。只有用户暂停、切后台、永久焦点丢失、播放结束、致命错误与释放才会清除。
     */
    @Volatile
    private var playIntended = false

    /** 是否已持有音频焦点，避免重复申请与重复放弃 */
    @Volatile
    private var audioFocusGranted = false

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

    /** 目标倍速：由播放线程在确认音频是否接受后写入，需保证可见性 */
    @Volatile
    private var playbackSpeed: Float = 1f

    /**
     * 最近一次确认生效的倍速，只在播放线程写入。
     * 没有音频轨（无法从 AudioTrack 读取实际倍速）时，作为切速失败的回退基准。
     */
    @Volatile
    private var appliedSpeed: Float = 1f

    private var needsStopBeforeRestart = false
    private var videoPlayer: FfmpegVideoPlayer? = null
    private var audioPlayer: FfmpegAudioPlayer? = null

    private val videoListener = object : PlayerListener {
        override fun onPrepared() {
            cachedDuration = readVideoDuration()
            postToMain {
                if (released) return@postToMain
                started = true
                ended = false
                // 准备是异步的：回调到达时页面可能已切后台、被用户暂停或丢失音频焦点。
                // 这里**不得**把 paused 置为 false，否则会把上述暂停状态覆盖掉，
                // 导致后台出声或用户暂停被自动取消。暂停的解除只能由 play() 触发。
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
            postPlayerError(nativeError(code, message, VideoPlayerErrorType.DECODER))
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
            postPlayerError(nativeError(code, message, VideoPlayerErrorType.AUDIO_OUTPUT))
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
        // 换源属于外部暂停场景：必须取消焦点续播标记与旧的播放意图，
        // 否则旧任务的焦点 Gain 会让新数据源意外自动播放
        playIntended = false
        pausedByFocusLoss = false
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
        // 目标倍速不在调用线程上乐观写入 playbackSpeed：连续两次请求且都被 AudioTrack 拒绝时，
        // 第二次会把尚未生效的第一个目标当成“旧倍速”回退，导致视频停在一个音频从未使用过的倍速。
        // 回退基准统一在播放线程上从音频实际倍速（无音频轨时为最近一次确认生效的倍速）读取。
        postPlayerAction {
            ensurePlayers()
            val audio = audioPlayer
            val hasAudio = audio != null && audioAvailable
            val fallbackSpeed = if (hasAudio) audio!!.playbackSpeed else appliedSpeed
            // 必须先确认音频能按目标倍速输出，再切换视频：
            // 若音频回退而视频已切速，两侧倍速不一致会造成持续音画不同步
            val audioApplied = !hasAudio || audio!!.setPlaybackSpeed(speed)
            if (!audioApplied) {
                appliedSpeed = fallbackSpeed
                playbackSpeed = fallbackSpeed
                videoPlayer?.setPlaybackSpeed(fallbackSpeed)
                logSpeedChangeFailure(speed, fallbackSpeed)
                postSpeedChanged(fallbackSpeed, false)
                return@postPlayerAction
            }
            appliedSpeed = speed
            playbackSpeed = speed
            videoPlayer?.setPlaybackSpeed(speed)
            postSpeedChanged(speed, true)
        }
    }

    override fun prepare() {
        listener?.onBuffering()
    }

    override fun play() {
        // 意图先于引擎状态记录：等待 Surface、准备中、缓冲中都不会改变它
        playIntended = true
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

    override fun pause() = pause(VideoPausedReason.USER)

    override fun pause(reason: VideoPausedReason) {
        pendingPlay = false
        // 除临时焦点丢失外的任何暂停都必须取消「焦点恢复后续播」，
        // 否则后续 AUDIOFOCUS_GAIN 会绕过用户意图自动播放
        if (!reason.allowsFocusGainResume()) {
            pausedByFocusLoss = false
        }
        // 用户暂停、永久焦点丢失、耳机拔出都视为「取消播放意图」，需要用户重新点击播放；
        // 切后台与 Surface 不可用保留意图，等条件满足后恢复
        if (reason.clearsPlayIntent()) {
            playIntended = false
        }
        if (!started || ended) {
            return
        }
        paused = true
        // 音频焦点丢失等场景由引擎内部直接调用 pause()，UI 侧收不到任何事件，
        // 这里统一上报并带上原因，保证 UI 状态、播放按钮与临时倍速一致
        postToMain {
            if (!released) {
                listener?.onPlaybackSuspended(reason)
            }
        }
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
        // 释放后不再接受任何恢复、焦点或延迟回调
        playIntended = false
        pausedByFocusLoss = false
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
        // surfaceCreated 与 surfaceChanged 会对同一个 Surface 重复回调，
        // 已经绑定过的相同 Surface 直接跳过，避免重复下发与重复触发起播
        val alreadyBound = valid && surfaceReady && surface === holderSurface
        surfaceReady = valid
        surface = if (valid) holderSurface else null
        if (!valid || alreadyBound) {
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
        if (!audioDrained) {
            if (paused || !surfaceReady) {
                // 预期内的暂停（用户暂停、页面 onPause、音频焦点丢失、Surface 等待重建）
                // 不是输出停滞。重置计时，恢复播放后重新开始判定，避免截断尾音。
                audioDrainProgressUptimeMs = SystemClock.uptimeMillis()
                return
            }
            if (!isAudioOutputStalled(audio)) {
                // 音频输出仍在推进（音轨可能长于视频轨），等待其真正播完，不截断尾音
                return
            }
        }
        completionNotified = true
        // 播放完成：清除意图与焦点续播标记，避免焦点 Gain 或返回前台重新播放
        playIntended = false
        pausedByFocusLoss = false
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

        // 与 setPlaybackSpeed() 保持一致：先确认音频倍速，再下发到视频
        val audio = audioPlayer
        var speed = playbackSpeed
        if (audio != null) {
            audio.setDataSource(source)
            if (!audio.setPlaybackSpeed(speed)) {
                val requestedSpeed = speed
                // 以音频实际生效的倍速为准，保证视频不会单独按目标倍速渲染
                speed = audio.playbackSpeed
                playbackSpeed = speed
                logSpeedChangeFailure(requestedSpeed, speed)
                postSpeedChanged(speed, false)
            }
        }
        appliedSpeed = speed

        video.setDataSource(source)
        video.setPlaybackSpeed(speed)
        surface?.let(video::setSurface)
    }

    private fun startPlayers() {
        // 申请失败时不得开始有声播放，直接按可重试的音频输出错误上报
        if (audioAvailable && !requestAudioFocus()) {
            postPlayerError(
                VideoPlaybackException(
                    VideoPlayerErrorType.AUDIO_OUTPUT,
                    "Audio focus request was denied."
                )
            )
            return
        }
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

    /**
     * 上报倍速请求的最终结果。倍速在播放线程上异步应用（需要先确认 AudioTrack 能否接受），
     * UI 必须以本回调为准更新按钮与选中态，避免把失败的请求当成已生效。
     */
    private fun postSpeedChanged(speed: Float, applied: Boolean) {
        postToMain {
            if (!released) {
                listener?.onPlaybackSpeedChanged(speed, applied)
            }
        }
    }

    /**
     * 记录倍速切换失败。音视频已一并保持在原倍速，播放可以继续，属于**可恢复警告**：
     * 只写日志并由 [postSpeedChanged] 通知 UI，绝不调用通用 onError，
     * 否则页面会被误判为播放失败而进入错误态。
     */
    private fun logSpeedChangeFailure(requestedSpeed: Float, keptSpeed: Float) {
        com.common.utils.LogUtil.e(
            TAG,
            "AudioTrack rejected playback speed $requestedSpeed, keep $keptSpeed"
        )
    }

    /**
     * 把 native 错误码归类为可区分的错误类型。
     * 只携带错误码与 native 文案，不拼接媒体地址，避免把带鉴权参数的 URL 写进日志与提示。
     */
    private fun nativeError(
        code: Int,
        message: String?,
        fallbackType: VideoPlayerErrorType,
    ): VideoPlaybackException {
        val type = when (code) {
            // native 侧 Surface 不可用
            NATIVE_ERROR_SURFACE_UNAVAILABLE -> VideoPlayerErrorType.SURFACE
            // 网络打开/探测/读取被中断或超时
            NATIVE_ERROR_IO_ABORTED -> VideoPlayerErrorType.NETWORK
            // 数据源打开失败（地址无效、无权限、格式无法解析）
            NATIVE_ERROR_OPEN_FAILED -> VideoPlayerErrorType.DATA_SOURCE
            // 播放中读取失败：连接重置、服务端断开、协议错误、重连耗尽
            NATIVE_ERROR_READ_FAILED -> VideoPlayerErrorType.NETWORK
            // 读到损坏数据无法继续解析
            NATIVE_ERROR_READ_INVALID_DATA -> VideoPlayerErrorType.DECODER
            // AudioTrack 创建/启动/切速/写入失败（-1001 ~ -1005）
            in NATIVE_ERROR_AUDIO_TRACK_LAST..NATIVE_ERROR_AUDIO_TRACK_FIRST ->
                VideoPlayerErrorType.AUDIO_OUTPUT

            else -> fallbackType
        }
        return VideoPlaybackException(type, "native error($code): ${message.orEmpty()}")
    }

    private fun postPlayerError(error: Throwable) {
        com.common.utils.LogUtil.e(TAG, "player error", error)
        pendingPlay = false
        // 致命错误：清除意图与焦点续播标记，等待用户点击重试，不得自动恢复
        playIntended = false
        pausedByFocusLoss = false
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

    /**
     * 申请音频焦点，返回是否申请成功。
     * 申请失败时调用方不得开始有声播放，否则会和正在占用焦点的应用抢声音。
     */
    private fun requestAudioFocus(): Boolean {
        if (audioFocusGranted) {
            return true
        }
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::requestAudioFocus)
                ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            // Android 8 以下同样要传入有效监听器，否则收不到焦点丢失事件
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        audioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return audioFocusGranted
    }

    private fun abandonAudioFocus() {
        if (!audioFocusGranted) {
            return
        }
        audioFocusGranted = false
        pausedByFocusLoss = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    /** 短暂失去焦点且允许降低音量时压低输出，恢复焦点后还原 */
    private fun applyAudioDucking(ducking: Boolean) {
        val volume = if (ducking) DUCK_VOLUME else 1f
        postPlayerAction { audioPlayer?.setVolume(volume) }
    }

    private fun postToMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}
