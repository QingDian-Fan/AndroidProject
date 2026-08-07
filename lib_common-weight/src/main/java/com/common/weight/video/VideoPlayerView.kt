package com.common.weight.video

import android.animation.ValueAnimator
import android.animation.ValueAnimator.ofInt
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.common.weight.R
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams


class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), OnClickListener,
    SeekBar.OnSeekBarChangeListener {

    private var engineFactory: VideoPlayerEngineFactory = defaultEngineFactory

    private var playerEngine: VideoPlayerEngine? = null

    private var videoPath: String? = null

    private var playbackSpeed: Float = 1f

    private val player: VideoPlayerEngine
        get() = ensurePlayer()

    companion object {

        @JvmStatic
        var defaultEngineFactory: VideoPlayerEngineFactory = ExoVideoPlayerEngineFactory

        /** UI 允许选择的常驻倍速档位，不接受档位之外的任意倍速 */
        private val SPEED_OPTIONS = floatArrayOf(0.75f, 1f, 1.25f, 1.5f, 2f, 3f)

        /** 长按临时加速使用的倍速 */
        private const val TEMPORARY_SPEED = 3f

        /** 倍速比较容差：引擎回调的实际倍速与档位常量之间可能存在浮点最低位误差 */
        private const val SPEED_EPSILON = 0.001f

        /**
         * 横向滑动整个播放器宽度对应的进度秒数。
         * 由原来的 60s 提升为 1.5 倍，相同滑动距离下调整秒数即为原来的 1.5 倍。
         */
        private const val SEEK_SECONDS_PER_WIDTH = 90f
    }


    private val surfaceView: SurfaceView by lazy { findViewById(R.id.surface_view) }

    private val topLayout: FrameLayout by lazy { findViewById(R.id.fl_top_layout) }
    private val btnActionBack: AppCompatImageView by lazy { findViewById(R.id.btn_action_back) }
    private val tvTitle: AppCompatTextView by lazy { findViewById(R.id.tv_title) }
    private val ivLock: AppCompatImageView by lazy { findViewById(R.id.iv_lock) }
    private val bottomLayout: LinearLayout by lazy { findViewById(R.id.ll_bottom_layout) }
    private val viewControl: VideoPlayButton by lazy { findViewById(R.id.view_control) }
    private val tvPlayerViewPlayTime: AppCompatTextView by lazy { findViewById(R.id.tv_player_view_play_time) }
    private val sbPlayerViewProgress: AppCompatSeekBar by lazy { findViewById(R.id.sb_player_view_progress) }
    private val tvPlayerViewTotalTime: AppCompatTextView by lazy { findViewById(R.id.tv_player_view_total_time) }
    private val tvPlayerViewSpeed: AppCompatTextView by lazy { findViewById(R.id.tv_player_view_speed) }
    private val ivPlayerViewOrientation: AppCompatImageView by lazy { findViewById(R.id.iv_player_view_orientation) }
    private val speedPanel: LinearLayout by lazy { findViewById(R.id.ll_player_view_speed_panel) }
    private val tvSpeedTip: AppCompatTextView by lazy { findViewById(R.id.tv_player_view_speed_tip) }
    private val errorLayout: LinearLayout by lazy { findViewById(R.id.ll_player_view_error) }
    private val tvError: AppCompatTextView by lazy { findViewById(R.id.tv_player_view_error) }
    private val tvRetry: AppCompatTextView by lazy { findViewById(R.id.tv_player_view_retry) }
    private val messageLayout: CardView by lazy { findViewById(R.id.cv_player_view_message) }
    private val ivMessage: AppCompatImageView by lazy { findViewById(R.id.iv_message) }
    private val tvMessage: AppCompatTextView by lazy { findViewById(R.id.tv_message) }
    private val pbLoading: ProgressBar by lazy { findViewById(R.id.pb_loading) }


    /** 是否锁定 */
    private var isLock = false

    /** 控制面板展示 */
    private var isControlPanelShow = false

    private var gestureEnabled = true

    /** 动画执行时间 */
    private val ANIM_TIME: Int = 500

    /** 面板隐藏间隔 */
    private val CONTROLLER_TIME: Int = 3000

    /** 当前播放进度 */
    private var currentProgress: Int = 0

    /** 是否正在拖动进度条：拖动期间只展示目标时间，不被后台刷新覆盖 */
    private var isDraggingProgress: Boolean = false

    /** 刷新间隔 */
    private val REFRESH_TIME: Int = 1000

    /** 音量管理器 */
    private val audioManager: AudioManager

    /** 最大音量值 */
    private var maxVoice: Int = 0

    /** 当前音量值 */
    private var currentVolume: Int = 0

    /** 当前亮度值 */
    private var currentBrightness: Float = 0f

    /** 当前窗口对象（用于调节屏幕亮度），默认从宿主 Activity 解析 */
    private var window: Window? = findHostActivity()?.window

    /** 调整秒数（仅用于提示展示，已按钳制后的目标位置换算） */
    private var adjustSecond: Int = 0

    /** 横向手势钳制后的目标进度（毫秒），-1 表示本次触摸序列没有待执行的定位 */
    private var adjustTargetMs: Int = -1

    /** 触摸方向 */
    private var touchOrientation: Int = -1

    /** 触摸按下的 X 坐标 */
    private var viewDownX: Float = 0f

    /** 触摸按下的 Y 坐标 */
    private var viewDownY: Float = 0f

    /** 提示对话框隐藏间隔 */
    private val DIALOG_TIME: Int = 500

    /** 播放器状态的单一来源 */
    private var playerState: VideoPlayerState = VideoPlayerState.IDLE

    /**
     * 用户期望的播放状态。缓冲中、等待 Surface 创建期间引擎的 isPlaying 为 false，
     * 但用户仍然期望继续播放，宿主必须据此判断回到前台后是否恢复，不能依赖瞬时 isPlaying。
     */
    private var desiredPlaying: Boolean = false

    /**
     * 当前生效的暂停原因；null 表示未处于暂停。
     *
     * 准备/缓冲期间暂停时 [playerState] 会先变成 PAUSED，但引擎的 Ready 回调仍会滞后到达，
     * [playerListener] 需要据此判断「该暂停是否仍然有效」，避免把播放器拉回 READY 假播放。
     */
    private var activePauseReason: VideoPausedReason? = null

    private var scaleType: VideoScaleType = VideoScaleType.RATIO_FILL_SIZE

    /** 最近一次回调的视频原始宽高，横竖屏切换后需要据此重新计算 Surface 尺寸 */
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    /** 当前是否处于横屏，仅用于同步方向按钮图标与无障碍描述 */
    private var isLandscape: Boolean = true

    /** 横竖屏切换入口是否可用，音频场景由宿主关闭 */
    private var orientationSwitchEnabled: Boolean = true

    /** 长按临时倍速是否生效 */
    private var temporarySpeedActive: Boolean = false

    /** 长按生效前的常驻倍速，松手后恢复到该值 */
    private var speedBeforeTemporary: Float = 1f

    /**
     * 已发出但尚未收到回调的临时倍速请求数。
     * 松手早于回调到达时，用它丢弃迟到的临时倍速回调，避免误改常驻倍速与按钮文本。
     */
    private var pendingTemporarySpeedCallbacks: Int = 0

    /** 本次触摸序列是否已经触发过长按：用于屏蔽随后的点击与手势 */
    private var longPressConsumed: Boolean = false

    /**
     * 布局中声明的基准内边距/外边距。
     * 必须在 init 之前声明：Insets 可能被多次分发，每次都要从基准值重新叠加，不能累加。
     */
    private var bottomBarBasePaddingStart: Int = 0
    private var bottomBarBasePaddingEnd: Int = 0
    private var bottomBarBasePaddingBottom: Int = 0
    private var speedPanelBaseMarginEnd: Int = 0
    private var speedPanelBaseMarginBottom: Int = 0
    private var speedTipBaseMarginTop: Int = 0

    private val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) = Unit

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) =
            Unit

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // 先恢复常驻倍速再交给引擎处理暂停与重建，避免重建后停留在临时倍速
            endTemporarySpeed()
        }
    }


    init {
        LayoutInflater.from(getContext()).inflate(R.layout.view_video_player, this, true)
        setOnClickListener(this)
        btnActionBack.setOnClickListener(this)
        ivLock.setOnClickListener(this)
        viewControl.setOnClickListener(this)
        tvPlayerViewSpeed.setOnClickListener(this)
        ivPlayerViewOrientation.setOnClickListener(this)
        tvRetry.setOnClickListener(this)
        sbPlayerViewProgress.setOnSeekBarChangeListener(this)
        // Surface 销毁属于临时倍速的结束场景，这里独立监听，不干扰引擎自身的 Surface 处理
        surfaceView.holder.addCallback(surfaceHolderCallback)
        updateSpeedLabel()
        applyOrientationIcon(isLandscape)
        recordInsetBaseValues()
        // 宿主为 edge-to-edge（setDecorFitsSystemWindows(false)），底部控制栏、倍速面板
        // 与顶部提示必须按安全区域动态避让，否则会落进手势热区、导航栏或显示切口
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            applySafeAreaInsets(insets)
            insets
        }
        ViewCompat.requestApplyInsets(this)

        audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)!!
    }

    // 播放引擎部分
    fun initData() {
        ensurePlayer()
    }

    fun setPlayerEngineFactory(factory: VideoPlayerEngineFactory): VideoPlayerView {
        engineFactory = factory
        if (playerEngine != null) {
            replacePlayer(factory.create(context))
        }
        return this
    }

    fun setPlayerEngine(engine: VideoPlayerEngine): VideoPlayerView {
        engineFactory = VideoPlayerEngineFactory { engine }
        replacePlayer(engine)
        return this
    }

    private fun ensurePlayer(): VideoPlayerEngine {
        val currentEngine = playerEngine
        if (currentEngine != null) {
            return currentEngine
        }
        return engineFactory.create(context).also { engine ->
            playerEngine = engine
            bindPlayer(engine)
            applyPlayerConfig(engine)
        }
    }

    private fun replacePlayer(newEngine: VideoPlayerEngine) {
        val oldEngine = playerEngine
        if (oldEngine === newEngine) {
            bindPlayer(newEngine)
            return
        }
        oldEngine?.setListener(null)
        oldEngine?.release()
        playerEngine = newEngine
        isPrepare = false
        bindPlayer(newEngine)
        applyPlayerConfig(newEngine)
    }

    private fun bindPlayer(engine: VideoPlayerEngine) {
        engine.attachSurface(surfaceView)
        engine.setListener(playerListener)
    }

    private fun applyPlayerConfig(engine: VideoPlayerEngine) {
        engine.setPlaybackSpeed(playbackSpeed)
        videoPath?.let(engine::setDataSource)
    }

    fun setTitle(title: String) {
        tvTitle.text = title
    }

    /**
     * 切换缩放模式。视频尺寸与容器尺寸已知时立即重算 Surface 布局；
     * 重复设置相同模式直接返回，避免无意义的布局请求。
     */
    fun setScaleType(scaleType: VideoScaleType) {
        if (this.scaleType == scaleType) {
            return
        }
        this.scaleType = scaleType
        applyScaleType(videoWidth, videoHeight)
    }

    /** 设置宿主窗口，用于亮度手势调节（默认会自动从 Activity 解析） */
    fun setWindow(window: Window) {
        this.window = window
    }

    /** 从 context 链中解析宿主 Activity */
    private fun findHostActivity(): Activity? {
        var ctx: Context? = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    /**
     * 请求常驻倍速。倍速可能由引擎异步应用，按钮文本与选中态在
     * [VideoPlayerEngine.Listener.onPlaybackSpeedChanged] 回调确认成功后才更新。
     */
    fun setSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    /**
     * 设置横竖屏切换入口是否可用。音频等不支持方向切换的场景必须关闭，
     * 否则用户与无障碍服务会看到一个可点击却无响应的方向控制。
     */
    fun setOrientationSwitchEnabled(enabled: Boolean) {
        orientationSwitchEnabled = enabled
        ivPlayerViewOrientation.visibility = if (enabled) VISIBLE else GONE
    }

    /**
     * 同步宿主的横竖屏状态：更新按钮图标与无障碍描述，
     * 并按“方向切换”结束长按临时倍速、收起倍速面板。
     */
    fun updateOrientation(isLandscape: Boolean) {
        this.isLandscape = isLandscape
        endTemporarySpeed()
        hideSpeedPanel()
        applyOrientationIcon(isLandscape)
    }

    private fun applyOrientationIcon(isLandscape: Boolean) {
        // 横屏时提供“切换竖屏”入口，竖屏时提供“切换横屏”入口
        val iconRes = if (isLandscape) {
            R.drawable.video_orientation_portrait_ic
        } else {
            R.drawable.video_orientation_landscape_ic
        }
        val descRes = if (isLandscape) {
            R.string.video_orientation_to_portrait
        } else {
            R.string.video_orientation_to_landscape
        }
        ivPlayerViewOrientation.setImageResource(iconRes)
        ivPlayerViewOrientation.contentDescription = context.getString(descRes)
    }

    //  倍速部分

    /** 倍速档位文本：0.75x / 1.0x / 1.25x / 1.5x / 2.0x / 3.0x */
    private fun formatSpeed(speed: Float): String {
        val hundredths = Math.round(speed * 100)
        val pattern = if (hundredths % 10 == 0) "%.1fx" else "%.2fx"
        return String.format(Locale.US, pattern, speed)
    }

    private fun isSameSpeed(left: Float, right: Float): Boolean =
        abs(left - right) < SPEED_EPSILON

    private fun updateSpeedLabel() {
        tvPlayerViewSpeed.text = formatSpeed(playbackSpeed)
    }

    private fun updateSpeedPanelSelection() {
        for (index in 0 until speedPanel.childCount) {
            val item = speedPanel.getChildAt(index)
            val speed = item.tag as? Float ?: continue
            item.isSelected = isSameSpeed(speed, playbackSpeed)
        }
    }

    private fun buildSpeedPanel() {
        if (speedPanel.childCount > 0) {
            return
        }
        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.dp_16)
        val paddingVertical = resources.getDimensionPixelSize(R.dimen.dp_8)
        val itemTextColor = ContextCompat.getColorStateList(context, R.color.video_speed_item_text)
        SPEED_OPTIONS.forEach { speed ->
            val item = AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tag = speed
                text = formatSpeed(speed)
                gravity = Gravity.CENTER
                setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimension(R.dimen.dp_14)
                )
                if (itemTextColor != null) {
                    setTextColor(itemTextColor)
                }
                setBackgroundResource(R.drawable.bg_video_speed_item)
                isClickable = true
                setOnClickListener { onSpeedItemClick(speed) }
            }
            speedPanel.addView(item)
        }
    }

    private fun onSpeedItemClick(speed: Float) {
        // 立即收起面板，实际生效与选中态以引擎回调为准
        hideSpeedPanel()
        removeCallbacks(mHideControllerRunnable)
        postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
        if (temporarySpeedActive) {
            // 长按临时加速期间不接受常驻倍速切换，避免松手后恢复到错误档位
            return
        }
        playerEngine?.setPlaybackSpeed(speed)
    }

    private fun toggleSpeedPanel() {
        if (speedPanel.isVisible) {
            hideSpeedPanel()
            return
        }
        buildSpeedPanel()
        updateSpeedPanelSelection()
        speedPanel.visibility = VISIBLE
    }

    private fun hideSpeedPanel() {
        if (speedPanel.isVisible) {
            speedPanel.visibility = GONE
        }
    }

    /**
     * 引擎回调的倍速结果。临时倍速与常驻倍速共用同一条回调，
     * 用 [pendingTemporarySpeedCallbacks] 区分，避免迟到的临时倍速回调污染常驻倍速。
     */
    private fun onPlaybackSpeedApplied(speed: Float, applied: Boolean) {
        if (pendingTemporarySpeedCallbacks > 0) {
            pendingTemporarySpeedCallbacks--
            if (!temporarySpeedActive) {
                // 松手早于回调到达：临时倍速已由 endTemporarySpeed() 恢复，这里只丢弃回调
                return
            }
            if (applied) {
                tvSpeedTip.visibility = VISIBLE
            } else {
                // 切速失败：引擎已把音视频一并保持在原倍速，不展示成功提示
                temporarySpeedActive = false
                tvSpeedTip.visibility = GONE
            }
            return
        }
        playbackSpeed = speed
        updateSpeedLabel()
        updateSpeedPanelSelection()
        if (!applied) {
            onSpeedChangeFailed?.invoke(speed)
        }
    }

    // 长按临时倍速部分

    private val mLongPressRunnable = Runnable { startTemporarySpeed() }

    /**
     * 长按临时加速的前置条件：非锁定、非拖动、处于正常播放中，且未进入其他手势。
     * 加载中、出错、播放完成、暂停状态下 [VideoPlayerEngine.isPlaying] 均为 false。
     */
    private fun canStartTemporarySpeed(): Boolean {
        val engine = playerEngine ?: return false
        return !isLock && !isDraggingProgress && touchOrientation == -1 &&
                playerState == VideoPlayerState.READY && engine.isPlaying && !engine.isEnded
    }

    private fun startTemporarySpeed() {
        if (temporarySpeedActive || !canStartTemporarySpeed()) {
            return
        }
        val engine = playerEngine ?: return
        speedBeforeTemporary = playbackSpeed
        temporarySpeedActive = true
        longPressConsumed = true
        pendingTemporarySpeedCallbacks++
        hideSpeedPanel()
        // 提示只在引擎确认切速成功后展示
        engine.setPlaybackSpeed(TEMPORARY_SPEED)
    }

    /**
     * 结束长按临时倍速并恢复长按前的常驻倍速。
     * 幂等：同一次长按的多个结束回调（松手、暂停、销毁、方向切换等）只恢复一次。
     */
    private fun endTemporarySpeed() {
        removeCallbacks(mLongPressRunnable)
        if (!temporarySpeedActive) {
            return
        }
        temporarySpeedActive = false
        tvSpeedTip.visibility = GONE
        playerEngine?.setPlaybackSpeed(speedBeforeTemporary)
    }

    fun setVideoPath(urlString: String) {
        // 同一地址重复下发会让引擎重新探测并重置全部状态，这里直接跳过
        if (videoPath == urlString && isPrepare && playerState != VideoPlayerState.ERROR) {
            return
        }
        videoPath = urlString
        isPrepare = false
        hideError()
        setState(VideoPlayerState.IDLE)
        player.setDataSource(urlString)
    }

    fun start(isStart: Boolean = true) {
        if (playerState == VideoPlayerState.RELEASED) {
            return
        }
        hideError()
        // 用户意图先于引擎实际状态记录：缓冲、等待 Surface 期间 isPlaying 仍为 false
        desiredPlaying = true
        // 重新发起播放：之前的暂停原因失效，后续 Ready 回调可正常进入 READY
        activePauseReason = null
        if (isStart) {
            setState(VideoPlayerState.BUFFERING)
            player.prepare()
        } else if (playerState == VideoPlayerState.PAUSED) {
            setState(VideoPlayerState.READY)
        }
        player.play()
        viewControl.play()
    }

    /**
     * 重试当前地址。复用已有引擎实例，不新建播放器，也不叠加刷新任务，
     * 因此不会出现多个播放器同时输出或多条进度刷新循环。
     */
    fun retry() {
        val path = videoPath
        if (path.isNullOrEmpty() || playerState == VideoPlayerState.RELEASED) {
            return
        }
        val engine = playerEngine ?: return
        hideError()
        isPrepare = false
        removeCallbacks(mRefreshRunnable)
        setState(VideoPlayerState.BUFFERING)
        engine.setDataSource(path)
        desiredPlaying = true
        // 重试是一次全新的播放请求，旧的暂停原因不再适用
        activePauseReason = null
        engine.prepare()
        engine.play()
        viewControl.play()
    }

    /** 当前播放器状态，宿主可据此决定页面级行为 */
    fun currentState(): VideoPlayerState = playerState

    /**
     * 用户是否期望继续播放。宿主切后台前应采样该值，而不是瞬时 [isPlaying]，
     * 否则缓冲中或等待 Surface 时会被误判为「未播放」。
     */
    fun isPlayIntended(): Boolean = desiredPlaying

    private fun setState(state: VideoPlayerState) {
        if (playerState == state) {
            // 幂等：重复的 onReady / onBuffering 回调不应叠加刷新任务或反复重排 UI
            return
        }
        playerState = state
        when (state) {
            VideoPlayerState.BUFFERING -> {
                pbLoading.visibility = VISIBLE
                hideSpeedPanel()
                topLayout.visibility = GONE
                bottomLayout.visibility = GONE
                ivLock.visibility = GONE
                viewControl.visibility = GONE
                removeCallbacks(mRefreshRunnable)
            }
            VideoPlayerState.READY -> {
                pbLoading.visibility = GONE
                // 先移除已有任务，避免多次进入播放态后叠加多个刷新循环
                removeCallbacks(mRefreshRunnable)
                postDelayed(mRefreshRunnable, (REFRESH_TIME / 2).toLong())
            }
            VideoPlayerState.ERROR -> {
                // 退出加载态并停止一切进行中的临时状态与刷新任务
                pbLoading.visibility = GONE
                hideSpeedPanel()
                endTemporarySpeed()
                desiredPlaying = false
                removeCallbacks(mRefreshRunnable)
                removeCallbacks(mHideControllerRunnable)
                removeCallbacks(mShowControllerRunnable)
                // 控制面板动画可能把顶部栏移出屏幕，错误态要保证返回入口可见可点
                cancelControlPanelAnimators()
                isControlPanelShow = false
                topLayout.translationY = 0f
                topLayout.visibility = VISIBLE
                bottomLayout.visibility = GONE
                ivLock.visibility = GONE
                viewControl.visibility = GONE
            }
            VideoPlayerState.RELEASED -> {
                pbLoading.visibility = GONE
                removeCallbacks(mRefreshRunnable)
            }
            else -> Unit
        }
        onStateChanged?.invoke(state)
    }

    private fun showError(error: VideoPlaybackException) {
        tvError.setText(errorMessageRes(error.errorType))
        errorLayout.visibility = VISIBLE
    }

    private fun hideError() {
        if (errorLayout.isVisible) {
            errorLayout.visibility = GONE
        }
    }

    private fun errorMessageRes(type: VideoPlayerErrorType): Int = when (type) {
        VideoPlayerErrorType.NETWORK -> R.string.video_error_network
        VideoPlayerErrorType.DATA_SOURCE -> R.string.video_error_data_source
        VideoPlayerErrorType.DECODER -> R.string.video_error_decoder
        VideoPlayerErrorType.SURFACE -> R.string.video_error_surface
        VideoPlayerErrorType.AUDIO_OUTPUT -> R.string.video_error_audio_output
        VideoPlayerErrorType.UNKNOWN -> R.string.video_error_unknown
    }

    @JvmOverloads
    fun pause(reason: VideoPausedReason = VideoPausedReason.USER) {
        // 暂停属于临时倍速的结束场景，必须先恢复常驻倍速
        endTemporarySpeed()
        // 只有用户主动暂停（及永久焦点丢失、耳机拔出）才清除播放意图；
        // 页面切后台保留意图，由宿主在返回前台时按原意图恢复（缓冲/等待 Surface 期间同样成立）
        if (reason.clearsPlayIntent()) {
            desiredPlaying = false
        }
        // 记录生效中的暂停原因：准备/缓冲期间暂停时状态仍是 BUFFERING，
        // 必须靠它拦截随后到达的 onReady()，否则会被覆盖成 READY 形成假播放
        activePauseReason = reason
        // 必须无条件下发：缓冲中、等待 Surface 时 isPlaying 为 false，
        // 但引擎内部仍保留待播放请求，只有 pause() 能取消它
        playerEngine?.pause(reason)
        viewControl.pause()
        // 准备中暂停同样要显式进入暂停态，不能停留在 BUFFERING
        if (playerState == VideoPlayerState.READY || playerState == VideoPlayerState.BUFFERING) {
            setState(VideoPlayerState.PAUSED)
        }
    }

    fun resume() {
        // 播放结束或失败后不自动重新开始，避免前后台切换造成重复播放
        if (playerState == VideoPlayerState.ENDED ||
            playerState == VideoPlayerState.ERROR ||
            playerState == VideoPlayerState.RELEASED ||
            player.isEnded
        ) {
            return
        }
        start(false)
    }

    fun isPlaying() = player.isPlaying
    fun isPrepare() = isPrepare

    fun destroy() {
        // 释放引擎前先结束临时倍速，保证同一次长按只恢复一次
        endTemporarySpeed()
        desiredPlaying = false
        setState(VideoPlayerState.RELEASED)
        hideSpeedPanel()
        hideError()
        cancelControlPanelAnimators()
        surfaceView.holder.removeCallback(surfaceHolderCallback)
        playerEngine?.setListener(null)
        playerEngine?.release()
        playerEngine = null
        removeCallbacks(mLongPressRunnable)
        removeCallbacks(mRefreshRunnable)
        removeCallbacks(mHideControllerRunnable)
        removeCallbacks(mShowControllerRunnable)
        removeCallbacks(mHideMessageRunnable)
        removeCallbacks(mShowMessageRunnable)
    }


    private fun safeDuration(): Long = player.duration

    private var isPrepare: Boolean = false

    private val playerListener = object : VideoPlayerEngine.Listener {
        override fun onBuffering() {
            if (playerState == VideoPlayerState.ERROR || playerState == VideoPlayerState.RELEASED) {
                // 错误态下引擎可能仍有滞后回调，不得把页面拉回加载态
                return
            }
            // 重新缓冲时已不在正常播放，临时倍速与其提示必须一并结束
            endTemporarySpeed()
            setState(VideoPlayerState.BUFFERING)
        }

        override fun onPlaybackSuspended(reason: VideoPausedReason) {
            // 引擎内部暂停（音频焦点丢失、耳机拔出等），UI 未主动发起，同样按结束场景处理
            endTemporarySpeed()
            if (playerState.isTerminal()) {
                // 更高优先级状态不得被延迟到达的暂停回调拉回暂停/播放态
                return
            }
            // 永久焦点丢失与耳机拔出必须由用户重新点击播放，这里同步清除播放意图
            if (reason.clearsPlayIntent()) {
                desiredPlaying = false
            }
            // INTERNAL（缓冲等）不改变用户意图，也不把 UI 拉成暂停态
            if (reason == VideoPausedReason.INTERNAL) {
                return
            }
            // 其余原因（焦点丢失、Surface、生命周期）UI 必须显示为暂停，
            // 不能停留在 READY 假播放；同时记录原因拦截随后到达的 Ready 回调
            activePauseReason = reason
            viewControl.pause()
            if (playerState == VideoPlayerState.READY || playerState == VideoPlayerState.BUFFERING) {
                setState(VideoPlayerState.PAUSED)
            }
        }

        override fun onPlaybackResumed() {
            // 引擎因临时焦点恢复而自动续播；用户主动暂停 / 永久焦点丢失后不得被覆盖
            if (!shouldAcceptAutoResume(playerState, activePauseReason)) {
                return
            }
            activePauseReason = null
            desiredPlaying = true
            viewControl.play()
            if (playerState == VideoPlayerState.PAUSED) {
                setState(VideoPlayerState.READY)
            }
        }

        override fun onReady() {
            val target = resolveReadyState(playerState, activePauseReason, desiredPlaying)
                ?: return // 终止态：不接受延迟到达的 Ready
            // 时长/进度条量程等准备信息与是否暂停无关，先同步
            if (!isPrepare) {
                onPrepared()
            }
            if (target == VideoPlayerState.PAUSED) {
                // 暂停仍然有效：只退出加载态，不启动播放态的进度刷新任务
                pbLoading.visibility = GONE
                viewControl.pause()
            }
            setState(target)
        }

        override fun onEnded() {
            if (playerState.isTerminal()) {
                // 幂等：引擎可能重复上报结束；ERROR 同样不得被延迟到达的完成回调覆盖，
                // 否则错误面板与重试入口会被「播放完成」顶掉
                return
            }
            // 播放完毕：按钮置为暂停（可播放）状态，并展示控制面板便于点击重播
            endTemporarySpeed()
            desiredPlaying = false
            setState(VideoPlayerState.ENDED)
            viewControl.pause()
            removeCallbacks(mRefreshRunnable)
            // 进度停在总时长，避免停留在最后一帧的时间戳上
            val duration = safeDuration()
            if (duration > 0 && !isDraggingProgress) {
                sbPlayerViewProgress.max = duration.toInt()
                sbPlayerViewProgress.progress = duration.toInt()
                tvPlayerViewPlayTime.text = conversionTime(duration)
            }
            post(mShowControllerRunnable)
            onCompletion?.invoke()
        }

        override fun onError(error: Throwable) {
            if (playerState == VideoPlayerState.ERROR || playerState == VideoPlayerState.RELEASED) {
                // 幂等：同一次失败可能由音视频两侧重复上报
                return
            }
            val playbackError = error as? VideoPlaybackException
                ?: VideoPlaybackException(
                    VideoPlayerErrorType.UNKNOWN,
                    error.message.orEmpty(),
                    error
                )
            setState(VideoPlayerState.ERROR)
            showError(playbackError)
            onError?.invoke(playbackError)
        }

        override fun onVideoSizeChanged(width: Int, height: Int) {
            videoWidth = width
            videoHeight = height
            applyScaleType(width, height)
        }

        override fun onPlaybackSpeedChanged(speed: Float, applied: Boolean) {
            onPlaybackSpeedApplied(speed, applied)
        }
    }

    private fun onPrepared() {
        isPrepare = true
        tvPlayerViewPlayTime.text = conversionTime(0)
        tvPlayerViewTotalTime.text = conversionTime(safeDuration())
        sbPlayerViewProgress.max = safeDuration().toInt()
    }

    //  UI部分
    override fun onClick(mView: View) {
        when (mView) {
            this -> {
                removeCallbacks(mHideControllerRunnable)
                removeCallbacks(mShowControllerRunnable)
                if (speedPanel.isVisible) {
                    // 倍速面板展开时，点击视频区域只收起面板，不改变控制面板状态
                    hideSpeedPanel()
                    postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
                    return
                }
                if (isControlPanelShow) {
                    // 隐藏控制面板
                    post(mHideControllerRunnable)
                    return
                }
                // 显示控制面板
                post(mShowControllerRunnable)
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
            }
            btnActionBack -> {
                onActionBack?.invoke()
            }
            tvRetry -> {
                retry()
            }
            tvPlayerViewSpeed -> {
                removeCallbacks(mHideControllerRunnable)
                toggleSpeedPanel()
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
            }
            ivPlayerViewOrientation -> {
                if (!orientationSwitchEnabled) {
                    return
                }
                hideSpeedPanel()
                removeCallbacks(mHideControllerRunnable)
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
                onOrientationSwitch?.invoke()
            }
            ivLock -> {
                isLock = !isLock
                hideSpeedPanel()
                ivLock.setImageResource(if (isLock) R.drawable.icon_video_lock_close else R.drawable.icon_video_lock_open)
                topLayout.visibility = if (isLock) GONE else VISIBLE
                bottomLayout.visibility = if (isLock) GONE else VISIBLE
                viewControl.visibility = if (isLock) GONE else VISIBLE
            }
            viewControl -> {
                if (viewControl.visibility != VISIBLE) {
                    return
                }
                if (player.isPlaying) {
                    pause()
                } else {
                    // 播放结束后再次点击，从头开始播放
                    if (player.isEnded) {
                        player.seekTo(0)
                    }
                    start(false)
                }
                // 先移除之前发送的
                removeCallbacks(mShowControllerRunnable)
                removeCallbacks(mHideControllerRunnable)
                // 重置显示隐藏面板任务
                if (!isControlPanelShow) {
                    post(mShowControllerRunnable)
                }
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
            }
        }
    }


    private val mHideControllerRunnable = Runnable {
        hideControlPanel()
    }
    private val mShowControllerRunnable = Runnable {
        showControlPanel()
    }

    /**
     * 显示提示
     */
    private val mShowMessageRunnable: Runnable = Runnable {
        hideControlPanel()
        messageLayout.visibility = VISIBLE
    }

    /**
     * 隐藏提示
     */
    private val mHideMessageRunnable: Runnable = Runnable { messageLayout.visibility = GONE }


    /**
     * 控制面板动画：快速点击时必须取消上一组，
     * 否则多个 Animator 会同时改写同一批 View 的位移与透明度。
     */
    private val controlPanelAnimators = ArrayList<ValueAnimator>(3)

    private fun cancelControlPanelAnimators() {
        if (controlPanelAnimators.isEmpty()) {
            return
        }
        // 先取出再取消：cancel() 回调中不会再访问正在遍历的集合
        val running = ArrayList(controlPanelAnimators)
        controlPanelAnimators.clear()
        running.forEach { it.cancel() }
    }

    private fun startControlPanelAnimator(animator: ValueAnimator) {
        controlPanelAnimators.add(animator)
        animator.start()
    }

    private fun hideControlPanel() {
        // 倍速面板依附于控制栏，控制栏收起时一并收起
        hideSpeedPanel()
        if (!isControlPanelShow) {
            return
        }
        isControlPanelShow = false
        cancelControlPanelAnimators()
        val topAnimator: ValueAnimator = ofInt(0, -topLayout.height)
        topAnimator.duration = ANIM_TIME.toLong()
        topAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            topLayout.translationY = translationY.toFloat()
            if (translationY != -topLayout.height) {
                return@addUpdateListener
            }
            if (topLayout.isInvisible) {
                topLayout.visibility = VISIBLE
            }
        }
        startControlPanelAnimator(topAnimator)
        val bottomAnimator: ValueAnimator = ofInt(0, bottomLayout.height)
        bottomAnimator.duration = ANIM_TIME.toLong()
        bottomAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            bottomLayout.translationY = translationY.toFloat()
            if (translationY != bottomLayout.height) {
                return@addUpdateListener
            }
            if (bottomLayout.isInvisible) {
                bottomLayout.visibility = VISIBLE
            }
        }
        startControlPanelAnimator(bottomAnimator)
        val alphaAnimator: ValueAnimator = ValueAnimator.ofFloat(1f, 0f)
        alphaAnimator.duration = ANIM_TIME.toLong()
        alphaAnimator.addUpdateListener {
            val alpha: Float = it.animatedValue as Float
            ivLock.alpha = alpha
            viewControl.alpha = alpha
            if (alpha != 0f) {
                return@addUpdateListener
            }
            if (ivLock.isVisible) {
                ivLock.visibility = INVISIBLE
            }
            if (viewControl.isVisible) {
                viewControl.visibility = INVISIBLE
            }
        }
        startControlPanelAnimator(alphaAnimator)
    }

    private fun showControlPanel() {
        if (isControlPanelShow ||
            playerState == VideoPlayerState.BUFFERING ||
            playerState == VideoPlayerState.ERROR ||
            playerState == VideoPlayerState.RELEASED
        ) {
            return
        }
        isControlPanelShow = true
        cancelControlPanelAnimators()

        if (topLayout.isGone && !isLock) {
            topLayout.visibility = VISIBLE
            bottomLayout.visibility = VISIBLE
            ivLock.visibility = VISIBLE
            viewControl.visibility = VISIBLE
        }


        val topAnimator: ValueAnimator = ofInt(-topLayout.height, 0)
        topAnimator.duration = ANIM_TIME.toLong()
        topAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            topLayout.translationY = translationY.toFloat()
            if (translationY != -topLayout.height) {
                return@addUpdateListener
            }
            if (topLayout.isInvisible) {
                topLayout.visibility = VISIBLE
            }
        }
        startControlPanelAnimator(topAnimator)
        val bottomAnimator: ValueAnimator = ofInt(bottomLayout.height, 0)
        bottomAnimator.duration = ANIM_TIME.toLong()
        bottomAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            bottomLayout.translationY = translationY.toFloat()
            if (translationY != bottomLayout.height) {
                return@addUpdateListener
            }
            if (bottomLayout.isInvisible) {
                bottomLayout.visibility = VISIBLE
            }
        }
        startControlPanelAnimator(bottomAnimator)
        val alphaAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
        alphaAnimator.duration = ANIM_TIME.toLong()
        alphaAnimator.addUpdateListener {
            val alpha: Float = it.animatedValue as Float
            ivLock.alpha = alpha
            viewControl.alpha = alpha
            if (alpha != 0f) {
                return@addUpdateListener
            }
            if (ivLock.isInvisible) {
                ivLock.visibility = VISIBLE
            }
            if (viewControl.isInvisible) {
                viewControl.visibility = VISIBLE
            }

        }
        startControlPanelAnimator(alphaAnimator)

    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 满足任一条件：关闭手势控制、处于锁定状态、处于缓冲状态
        if (!gestureEnabled || isLock) {
            return super.onTouchEvent(event)
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                maxVoice = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (window != null) {
                    currentBrightness = window!!.attributes.screenBrightness
                    // 如果当前亮度是默认的，那么就获取系统当前的屏幕亮度
                    if (currentBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) {
                        currentBrightness = try {
                            // 这里需要注意，Settings.System.SCREEN_BRIGHTNESS 获取到的值在小米手机上面会超过 255
                            min(
                                Settings.System.getInt(
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS
                                ), 255
                            ) / 255f
                        } catch (ignored: Settings.SettingNotFoundException) {
                            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
                        }
                    }
                }
                viewDownX = event.x
                viewDownY = event.y
                longPressConsumed = false
                // 新的触摸序列开始，清空上一次可能残留的定位目标，避免跳到旧预览位置
                adjustTargetMs = -1
                adjustSecond = 0
                removeCallbacks(mHideControllerRunnable)
                // 长按临时加速：使用系统长按判定时间，避免自定义过短时间造成误触
                removeCallbacks(mLongPressRunnable)
                if (canStartTemporarySpeed()) {
                    postDelayed(
                        mLongPressRunnable,
                        ViewConfiguration.getLongPressTimeout().toLong()
                    )
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 触摸转为多指操作：取消长按判定并结束临时倍速
                endTemporarySpeed()
            }
            MotionEvent.ACTION_MOVE -> run {
                if (event.pointerCount > 1) {
                    endTemporarySpeed()
                    return@run
                }
                if (temporarySpeedActive) {
                    // 长按已生效：本次触摸序列不再触发快进/快退与亮度/音量手势
                    return@run
                }
                // 计算偏移的距离（按下的位置 - 当前触摸的位置）
                val distanceX: Float = viewDownX - event.x
                val distanceY: Float = viewDownY - event.y
                val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
                // 手指偏移的距离一定不能太短，这个是前提条件。
                // 横向与纵向任一方向达到 touch slop 即可进入手势判定，
                // 否则横向快进/快退会被迫先产生明显纵向位移才能识别。
                if (abs(distanceX) < touchSlop && abs(distanceY) < touchSlop) {
                    return@run
                }
                // 已明显移动：取消尚未生效的长按判定，交给快进/快退或亮度/音量手势
                removeCallbacks(mLongPressRunnable)
                if (touchOrientation == -1) {
                    // 判断滚动方向是垂直的还是水平的
                    if (abs(distanceY) > abs(distanceX)) {
                        touchOrientation = LinearLayout.VERTICAL
                    } else if (abs(distanceY) < abs(distanceX)) {
                        touchOrientation = LinearLayout.HORIZONTAL
                    }
                }

                // 如果手指触摸方向是水平的
                if (touchOrientation == LinearLayout.HORIZONTAL) {
                    val duration: Int = getDuration()
                    if (duration <= 0) {
                        // 总时长未知时无法定位，直接忽略本次横向手势
                        return@run
                    }
                    val basePosition: Int = getProgress()
                    val second: Int =
                        (-(distanceX / width.toFloat() * SEEK_SECONDS_PER_WIDTH)).toInt()
                    // 每次移动都把目标位置钳制到 [0, duration]：越界时停在起点/终点，
                    // 而不是丢弃本次预览、把上一次的有效值一直保留到抬手
                    val target: Int = (basePosition + second * 1000).coerceIn(0, duration)
                    adjustTargetMs = target
                    // 提示秒数按钳制后的目标重新计算，保证与实际会跳转到的位置一致
                    adjustSecond = (target - basePosition) / 1000
                    ivMessage.setImageResource(if (adjustSecond < 0) R.drawable.video_schedule_rewind_ic else R.drawable.video_schedule_forward_ic)
                    tvMessage.text = String.format("%s s", abs(adjustSecond))
                    post(mShowMessageRunnable)
                    return@run
                }

                // 如果手指触摸方向是垂直的
                if (touchOrientation == LinearLayout.VERTICAL) {
                    // 判断触摸点是在屏幕左边还是右边
                    if (event.x.toInt() < width / 2) {
                        // 手指在屏幕左边
                        val delta: Float =
                            (distanceY / height) * WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                        if (delta == 0f) {
                            return@run
                        }

                        // 更新系统亮度
                        val brightness: Float = min(
                            max(
                                currentBrightness + delta,
                                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF
                            ),
                            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                        )
                        window?.apply {
                            val attributes: WindowManager.LayoutParams = attributes
                            attributes.screenBrightness = brightness
                            setAttributes(attributes)
                        }
                        val percent: Int = (brightness * 100).toInt()
                        @DrawableRes val iconId: Int = when {
                            percent > 100 / 3 * 2 -> {
                                R.drawable.video_brightness_high_ic
                            }
                            percent > 100 / 3 -> {
                                R.drawable.video_brightness_medium_ic
                            }
                            else -> {
                                R.drawable.video_brightness_low_ic
                            }
                        }
                        ivMessage.setImageResource(iconId)
                        tvMessage.text = String.format("%s %%", percent)
                        post(mShowMessageRunnable)
                        return@run
                    }

                    // 手指在屏幕右边
                    val delta: Float = (distanceY / height) * maxVoice
                    if (delta == 0f) {
                        return@run
                    }

                    // 更新系统音量
                    val voice: Int = min(max(currentVolume + delta, 0f), maxVoice.toFloat()).toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, voice, 0)
                    val percent: Int = voice * 100 / maxVoice
                    @DrawableRes val iconId: Int
                    iconId = when {
                        percent > 100 / 3 * 2 -> {
                            R.drawable.video_volume_high_ic
                        }
                        percent > 100 / 3 -> {
                            R.drawable.video_volume_medium_ic
                        }
                        percent != 0 -> {
                            R.drawable.video_volume_low_ic
                        }
                        else -> {
                            R.drawable.video_volume_mute_ic
                        }
                    }
                    ivMessage.setImageResource(iconId)
                    tvMessage.text = String.format("%s %%", percent)
                    post(mShowMessageRunnable)
                    return@run
                }
            }
            MotionEvent.ACTION_UP -> {
                // 结束临时倍速并恢复常驻倍速；长按已生效时不再派发点击、也不再执行 seek
                val consumedByLongPress = longPressConsumed
                endTemporarySpeed()
                if (!consumedByLongPress &&
                    abs(viewDownX - event.x) <= ViewConfiguration.get(context).scaledTouchSlop &&
                    abs(viewDownY - event.y) <= ViewConfiguration.get(context).scaledTouchSlop
                ) {
                    // 如果整个视频播放区域太大，触摸移动会导致触发点击事件，所以这里换成手动派发点击事件
                    if (isEnabled && isClickable) {
                        performClick()
                    }
                }
                touchOrientation = -1
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                applyPendingSeek()
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
                post(mHideMessageRunnable)
                // postDelayed(mHideMessageRunnable, DIALOG_TIME.toLong())
            }
            MotionEvent.ACTION_CANCEL -> {
                endTemporarySpeed()
                touchOrientation = -1
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                applyPendingSeek()
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
                post(mHideMessageRunnable)

                // postDelayed(mHideMessageRunnable, DIALOG_TIME.toLong())
            }
        }
        return true
    }

    private fun getDuration(): Int = safeDuration().toInt()

    private fun getProgress(): Int = player.currentPosition.toInt()

    /**
     * 抬手/取消时执行本次横向手势累积的定位。
     * 目标位置在手势过程中已钳制到 [0, 总时长]，这里只执行一次，且执行后立即清空，
     * 保证同一次触摸序列不会重复 seek。
     */
    private fun applyPendingSeek() {
        val target = adjustTargetMs
        adjustTargetMs = -1
        adjustSecond = 0
        if (target < 0) {
            return
        }
        setProgress(target)
    }


    /** 返回事件回调 */
    var onActionBack: (() -> Unit)? = null

    /** 播放完成回调 */
    var onCompletion: (() -> Unit)? = null

    var onError: ((Throwable) -> Unit)? = null

    /** 横竖屏切换按钮点击回调，由宿主 Activity 决定实际方向请求 */
    var onOrientationSwitch: (() -> Unit)? = null

    /** 常驻倍速切换失败回调，参数为回退后仍在生效的倍速。属于可恢复警告，不会进入错误态 */
    var onSpeedChangeFailed: ((Float) -> Unit)? = null

    /** 播放器状态变化回调，宿主可据此做页面级处理 */
    var onStateChanged: ((VideoPlayerState) -> Unit)? = null

    /** 时间格式化的复用缓冲区与格式化器（仅主线程访问） */
    private val timeBuilder = StringBuilder()
    private val timeFormatter = Formatter(timeBuilder, Locale.getDefault())

    /**
     * 时间转换
     */
    private fun conversionTime(time: Long): String {
        // 进度每秒刷新一次，复用 StringBuilder 与 Formatter，避免高频临时对象分配
        timeBuilder.setLength(0)
        // 总秒数
        val totalSeconds: Long = time / 1000
        // 小时数
        val hours: Long = totalSeconds / 3600
        // 分钟数
        val minutes: Long = (totalSeconds / 60) % 60
        // 秒数
        val seconds: Long = totalSeconds % 60
        return if (hours > 0) {
            timeFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            timeFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            tvPlayerViewPlayTime.text = conversionTime(progress.toLong())
            return
        }
        if (progress != 0) {
            // 记录当前播放进度
            currentProgress = progress
        } else {
            // 如果 Activity 返回到后台，progress 会等于 0，而 mVideoView.getDuration 会等于 -1
            // 所以要避免在这种情况下记录当前的播放进度，以便用户从后台返回到前台的时候恢复正确的播放进度
            if (player.duration > 0) {
                currentProgress = progress
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        // 拖动期间暂停刷新，进度条与时间只跟随手指
        isDraggingProgress = true
        removeCallbacks(mRefreshRunnable)
        removeCallbacks(mHideControllerRunnable)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        isDraggingProgress = false
        postDelayed(mRefreshRunnable, REFRESH_TIME.toLong())
        postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
        // 设置选择的播放进度
        setProgress(seekBar.progress)
    }


    /**
     * 刷新任务
     */
    private val mRefreshRunnable: Runnable = object : Runnable {

        override fun run() {
            postDelayed(this, REFRESH_TIME.toLong())
            if (isDraggingProgress) {
                // 拖动期间只展示目标时间，不被主时钟覆盖
                return
            }
            val duration = safeDuration()
            if (duration > 0 && sbPlayerViewProgress.max != duration.toInt()) {
                // 时长可能在首帧之后才拿到，这里同步刷新总时长与进度条量程
                sbPlayerViewProgress.max = duration.toInt()
                tvPlayerViewTotalTime.text = conversionTime(duration)
            }
            val progress = clampProgress(player.currentPosition)
            tvPlayerViewPlayTime.text = conversionTime(progress)
            sbPlayerViewProgress.progress = progress.toInt()
            sbPlayerViewProgress.secondaryProgress = clampProgress(player.bufferedPosition).toInt()
        }
    }

    /** 播放位置限制在 [0, 总时长] 内，避免进度条越界 */
    private fun clampProgress(position: Long): Long {
        val duration = safeDuration()
        val safePosition = max(position, 0L)
        return if (duration > 0) min(safePosition, duration) else safePosition
    }

    /**
     * 设置视频播放进度，保持拖动前的播放/暂停状态
     */
    private fun setProgress(progress: Int) {
        val finalProgress: Int = clampProgress(progress.toLong()).toInt()
        player.seekTo(finalProgress.toLong())
        sbPlayerViewProgress.progress = finalProgress
        tvPlayerViewPlayTime.text = conversionTime(finalProgress.toLong())
    }

    //  安全区域适配

    private fun recordInsetBaseValues() {
        bottomBarBasePaddingStart = bottomLayout.paddingStart
        bottomBarBasePaddingEnd = bottomLayout.paddingEnd
        bottomBarBasePaddingBottom = bottomLayout.paddingBottom
        (speedPanel.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            speedPanelBaseMarginEnd = it.marginEnd
            speedPanelBaseMarginBottom = it.bottomMargin
        }
        (tvSpeedTip.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            speedTipBaseMarginTop = it.topMargin
        }
    }

    /**
     * 按安全区域避让：
     * 底部控制栏与倍速面板同时避开系统手势热区、导航栏与显示切口；
     * 顶部长按提示避开状态栏与显示切口。系统栏被隐藏时对应 Insets 为 0，
     * 手势热区与切口 Insets 仍然存在，因此三者取并集。
     */
    private fun applySafeAreaInsets(insets: WindowInsetsCompat) {
        val bottomSafe = insets.getInsets(
            WindowInsetsCompat.Type.systemGestures() or
                    WindowInsetsCompat.Type.navigationBars() or
                    WindowInsetsCompat.Type.displayCutout()
        )
        val topSafe = insets.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
        )
        bottomLayout.setPaddingRelative(
            bottomBarBasePaddingStart + bottomSafe.left,
            bottomLayout.paddingTop,
            bottomBarBasePaddingEnd + bottomSafe.right,
            bottomBarBasePaddingBottom + bottomSafe.bottom
        )
        speedPanel.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginEnd = speedPanelBaseMarginEnd + bottomSafe.right
            bottomMargin = speedPanelBaseMarginBottom + bottomSafe.bottom
        }
        tvSpeedTip.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = speedTipBaseMarginTop + topSafe.top
        }
    }

    /** 容器尺寸变化（典型场景为横竖屏切换）后按新的宽高重新计算 Surface 尺寸 */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyScaleType(videoWidth, videoHeight)
    }

    private fun applyScaleType(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0 || width <= 0 || height <= 0) {
            return
        }
        when (scaleType) {
            VideoScaleType.ORIGINAL_SIZE -> {
                val originWidth = if (videoWidth > width) {
                    width
                } else {
                    videoWidth
                }
                val originHeight = if (videoHeight > height) {
                    height
                } else {
                    videoHeight
                }
                val layoutPrams = LayoutParams(originWidth, originHeight)
                layoutPrams.gravity = 0x11
                surfaceView.layoutParams = layoutPrams
            }
            VideoScaleType.FULL_SIZE -> {
                val layoutPrams = LayoutParams(width, height)
                layoutPrams.gravity = 0x11
                surfaceView.layoutParams = layoutPrams
            }
            else -> {
                // 等比缩放到容器内并居中：按宽高中较小的缩放比取整，
                // 横屏下与原来的按高度换算结果一致，竖屏下不会因宽度溢出而被裁切。
                val scale = min(
                    width.toFloat() / videoWidth.toFloat(),
                    height.toFloat() / videoHeight.toFloat()
                )
                val targetWidth = max((videoWidth * scale).toInt(), 1)
                val targetHeight = max((videoHeight * scale).toInt(), 1)
                val layoutPrams = LayoutParams(targetWidth, targetHeight)
                layoutPrams.gravity = 0x11
                surfaceView.layoutParams = layoutPrams
            }
        }

    }


}
