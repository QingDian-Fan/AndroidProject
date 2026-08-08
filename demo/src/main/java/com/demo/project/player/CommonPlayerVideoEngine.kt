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
import com.demo.project.BuildConfig
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

        /** Debug 统计输出间隔，避免高频日志 */
        private const val STATS_LOG_INTERVAL_MS = 1000L

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

        /** 送包 / 取帧 / 重采样等解码环节不可恢复失败 */
        private const val NATIVE_ERROR_DECODE_FAILED = -1104

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

    /**
     * 唯一逻辑媒体时钟、公开进度与完成聚合的权威来源。
     * 只在播放线程（clockRunnable / postPlayerAction）上访问。
     *
     * 进度单调性、音频结束后的自由推进、完成条件与完成幂等全部由它负责，
     * Engine 不再保留 videoDecodeFinished / audioDrain* / completionNotified 等重复状态。
     */
    private val playbackClock = PlaybackClock()

    /** 上一次输出 Debug 统计的时刻，仅播放线程访问 */
    private var lastStatsLogUptimeMs = 0L

    /**
     * 是否已上报致命错误。音视频两条线程可能先后失败，也可能一路失败、另一路随后读到 EOF，
     * 该标记保证只上报一次 ERROR，且错误之后不再上报播放完成。
     */
    @Volatile
    private var errorNotified = false

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
            // 视频解码线程结束只是一个事件，是否真的播完由 PlaybackClock 依据
            // 各层排空快照统一聚合（native 的 isDecoderDrained 才是权威来源）。
            // 这里只唤醒一次时钟采样，避免等到下一个采样周期。
            postPlayerAction { updateClock() }
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
            // 同上：音频解码线程结束不代表 AudioTrack 已经播完，交给统一聚合判断
            postPlayerAction { updateClock() }
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
            // 永久焦点丢失时已 abandonAudioFocus()，恢复播放必须重新申请，
            // 否则会在别的应用持有焦点的情况下直接恢复 AudioTrack 输出造成同时发声。
            // requestAudioFocus() 内部对「已持有」是幂等的，普通暂停恢复不会重复申请。
            if (audioAvailable && !requestAudioFocus()) {
                // 申请失败：保持暂停并按可重试的音频输出错误上报，不得恢复出声
                paused = true
                pendingPlay = false
                postPlayerError(
                    VideoPlaybackException(
                        VideoPlayerErrorType.AUDIO_OUTPUT,
                        "Audio focus request was denied."
                    )
                )
                return
            }
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
        val resumeAt = pendingSeekMs
        pendingSeekMs = -1L
        // 新一轮会话从 resumeAt 开始计时，时钟不得沿用上一轮的进度与完成标记
        resetPlaybackState(resumeAt.coerceAtLeast(0L))
        if (resumeAt > 0L) {
            // 重启后解码器需要重新定位，这段时间内对外保持目标位置
            cachedPosition = resumeAt
            seekTargetMs = resumeAt
            seekStartUptimeMs = SystemClock.uptimeMillis()
            playbackClock.beginSeek(resumeAt)
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
            // 用户主动 seek 是唯一允许进度跳变的场景：告知时钟重建锚点，
            // 并丢弃旧位置的采样（旧 packet / PCM / 视频帧由 native 按 seek 序号丢弃）
            playbackClock.beginSeek(target)
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

    private fun resetPlaybackState(startPositionMs: Long = 0L) {
        // 重试 / 换源属于全新一轮播放，允许重新上报错误
        errorNotified = false
        audioAvailable = true
        seekTargetMs = -1L
        // 新一轮会话：时钟锚点、进度单调性与完成标记一并复位
        playbackClock.reset(startPositionMs)
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

        if (playbackClock.durationMs <= 0L) {
            val duration = runCatching { video.duration }.getOrDefault(0L)
            playbackClock.setDuration(duration)
            if (duration > 0L) {
                cachedDuration = duration
            }
        }

        // seek 落点确认：解码器不再 seeking，或保护超时
        if (playbackClock.isSeeking) {
            val stillSeeking = runCatching { video.isSeeking }.getOrDefault(false) ||
                    (audio != null && audioAvailable &&
                            runCatching { audio.isSeeking }.getOrDefault(false))
            val expired = SystemClock.uptimeMillis() - seekStartUptimeMs > SEEK_TIMEOUT_MS
            if (!stillSeeking || expired) {
                playbackClock.endSeek()
                seekTargetMs = -1L
            }
        }

        val hasAudioOutput = audio != null && audioAvailable && audio.isOutputActive
        // 音频时钟必须来自 AudioTrack **实际播放头**，不能用解码进度或写入进度冒充
        val audioOutputPosition = if (hasAudioOutput) {
            runCatching { audio!!.outputPositionMs }.getOrDefault(-1L)
        } else {
            -1L
        }
        val videoPosition = runCatching { video.currentPosition }.getOrDefault(-1L)
        val hasAudio = audioAvailable && audio != null

        val drain = DrainSnapshot(
            videoDecoderDrained = runCatching { video.isDecoderDrained }.getOrDefault(false),
            hasAudio = hasAudio,
            audioDecoderDrained = !hasAudio ||
                    runCatching { audio!!.isDecoderDrained }.getOrDefault(false),
            audioOutputDrained = !hasAudio ||
                    runCatching { audio!!.isOutputDrained }.getOrDefault(true),
        )

        val output = playbackClock.update(
            ClockInput(
                audioOutputPositionMs = audioOutputPosition,
                videoPositionMs = videoPosition,
                speed = playbackSpeed,
                wallClockMs = SystemClock.uptimeMillis(),
                drain = drain,
                // 已上报致命错误时禁止判定完成
                terminated = errorNotified,
            )
        )
        cachedPosition = output.positionMs
        if (output.masterClockMs >= 0L) {
            runCatching { video.setMasterClock(output.masterClockMs) }
        } else {
            // 仅无音轨媒体会走到这里：视频使用自身 PTS 时钟
            runCatching { video.clearMasterClock() }
        }
        if (output.shouldComplete) {
            finishPlayback()
        }
        logPlaybackStats(video, audio, output, audioOutputPosition, videoPosition, drain)
    }

    /**
     * Debug 受控统计。Release 不输出（[BuildConfig.isDebug] 为 false 时直接返回），
     * 且只输出播放器内部指标，不包含媒体地址与鉴权参数。
     */
    private fun logPlaybackStats(
        video: FfmpegVideoPlayer,
        audio: FfmpegAudioPlayer?,
        output: ClockOutput,
        audioOutputPosition: Long,
        videoPosition: Long,
        drain: DrainSnapshot,
    ) {
        if (!BuildConfig.isDebug) {
            return
        }
        val now = SystemClock.uptimeMillis()
        if (now - lastStatsLogUptimeMs < STATS_LOG_INTERVAL_MS) {
            return
        }
        lastStatsLogUptimeMs = now

        // 音视频时钟差必须基于「实际展示帧 PTS」与「AudioTrack 实际输出位置」，
        // 而不是两个解码线程的进度
        val syncDiff = if (audioOutputPosition >= 0L && videoPosition >= 0L) {
            videoPosition - audioOutputPosition
        } else {
            Long.MIN_VALUE
        }
        val stats = runCatching { video.frameStats }.getOrNull()
        val diagnosis = stats?.let {
            FrameDropAnalyzer.analyze(
                decodedFrames = it.decodedFrames,
                renderedFrames = it.renderedFrames,
                maxConsecutiveDrops = it.maxConsecutiveDrops,
                // 屏幕展示能力按 60Hz 估算本次采样窗口的上限
                displayCapacityFrames = it.renderedFrames,
            )
        }
        val builder = StringBuilder(256)
            .append("speed req=").append(playbackSpeed)
            .append(" applied=").append(appliedSpeed)
            .append(" | master=").append(output.masterClockMs)
            .append(" public=").append(output.positionMs)
            .append(" videoPts=").append(videoPosition)
            .append(" audioOut=").append(audioOutputPosition)
        if (syncDiff != Long.MIN_VALUE) {
            builder.append(" syncDiff=").append(syncDiff).append("ms")
        }
        if (audio != null) {
            builder.append(" | written=").append(runCatching { audio.writtenFrames }.getOrDefault(-1L))
                .append(" played=").append(runCatching { audio.playedFrames }.getOrDefault(-1L))
        }
        builder.append(" | drain v=").append(drain.videoDecoderDrained)
            .append(" aDec=").append(drain.audioDecoderDrained)
            .append(" aOut=").append(drain.audioOutputDrained)
        if (stats != null) {
            builder.append(" | ").append(stats)
        }
        if (diagnosis != null) {
            builder.append(" verdict=").append(diagnosis.verdict)
        }
        builder.append(" | seeking=").append(playbackClock.isSeeking)
            .append(" completed=").append(playbackClock.completed)
        com.common.utils.LogUtil.d(TAG, builder.toString())
    }

    /**
     * 上报播放完成。完成条件由 [PlaybackClock] 统一聚合（视频解码器排空 +
     * 音频解码器/SWR 排空 + AudioTrack 播完，或媒体无音轨），
     * [PlaybackClock.markCompleted] 保证同一会话只执行一次。
     */
    private fun finishPlayback() {
        if (released || !playbackClock.markCompleted()) {
            return
        }
        // 播放完成：清除意图与焦点续播标记，避免焦点 Gain 或返回前台重新播放
        playIntended = false
        pausedByFocusLoss = false
        clockRunning = false
        playerHandler.removeCallbacks(clockRunnable)
        cachedPosition = playbackClock.positionMs
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
            // 送包 / 取帧 / 重采样等解码环节失败
            NATIVE_ERROR_DECODE_FAILED -> VideoPlayerErrorType.DECODER
            // AudioTrack 创建/启动/切速/写入失败（-1001 ~ -1005）
            in NATIVE_ERROR_AUDIO_TRACK_LAST..NATIVE_ERROR_AUDIO_TRACK_FIRST ->
                VideoPlayerErrorType.AUDIO_OUTPUT

            else -> fallbackType
        }
        return VideoPlaybackException(type, "native error($code): ${message.orEmpty()}")
    }

    private fun postPlayerError(error: Throwable) {
        // 幂等：音视频两条线程可能同时失败，UI 只能进入一次 ERROR
        if (errorNotified) {
            return
        }
        errorNotified = true
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
