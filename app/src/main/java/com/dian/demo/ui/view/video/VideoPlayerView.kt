package com.dian.demo.ui.view.video

import android.animation.ValueAnimator
import android.animation.ValueAnimator.ofInt
import android.content.Context
import android.content.res.Resources
import android.media.AudioManager
import android.provider.Settings
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
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
import com.demo.project.utils.ext.centerInParent
import com.demo.project.utils.ext.center_horizontal
import com.demo.project.utils.ext.layout_gravity
import com.dian.demo.R
import com.dian.demo.utils.aop.SingleClick
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), OnClickListener,
    IMediaPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener,
    IMediaPlayer.OnVideoSizeChangedListener {

    private val player by lazy { IjkMediaPlayer() }


    private val surfaceView: SurfaceView by lazy { findViewById<SurfaceView>(R.id.surface_view) }
    private val topLayout: FrameLayout by lazy { findViewById<FrameLayout>(R.id.fl_top_layout) }
    private val btnActionBack: AppCompatImageView by lazy { findViewById<AppCompatImageView>(R.id.btn_action_back) }
    private val tvTitle: AppCompatTextView by lazy { findViewById<AppCompatTextView>(R.id.tv_title) }
    private val ivLock: AppCompatImageView by lazy { findViewById<AppCompatImageView>(R.id.iv_lock) }
    private val bottomLayout: LinearLayout by lazy { findViewById<LinearLayout>(R.id.ll_bottom_layout) }
    private val viewControl: VideoPlayButton by lazy { findViewById<VideoPlayButton>(R.id.view_control) }
    private val tvPlayerViewPlayTime: AppCompatTextView by lazy { findViewById<AppCompatTextView>(R.id.tv_player_view_play_time) }
    private val sbPlayerViewProgress: AppCompatSeekBar by lazy { findViewById<AppCompatSeekBar>(R.id.sb_player_view_progress) }
    private val tvPlayerViewTotalTime: AppCompatTextView by lazy { findViewById<AppCompatTextView>(R.id.tv_player_view_total_time) }


    private val messageLayout: CardView by lazy { findViewById<CardView>(R.id.cv_player_view_message) }
    private val ivMessage: AppCompatImageView by lazy { findViewById<AppCompatImageView>(R.id.iv_message) }
    private val tvMessage: AppCompatTextView by lazy { findViewById<AppCompatTextView>(R.id.tv_message) }
    private val pbLoading: ProgressBar by lazy { findViewById<ProgressBar>(R.id.pb_loading) }


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

    /** 当前窗口对象 */
    private var window: Window? = null

    /** 调整秒数 */
    private var adjustSecond: Int = 0

    /** 触摸方向 */
    private var touchOrientation: Int = -1

    /** 触摸按下的 X 坐标 */
    private var viewDownX: Float = 0f

    /** 触摸按下的 Y 坐标 */
    private var viewDownY: Float = 0f

    /** 提示对话框隐藏间隔 */
    private val DIALOG_TIME: Int = 500

    private val STATUS_LOADING = 1
    private val STATUS_PLAYING = 2

    private var VIDEO_STATUS = STATUS_LOADING

    private var scaleType: VideoScaleType = VideoScaleType.RATIO_FILL_SIZE


    init {
        LayoutInflater.from(getContext()).inflate(R.layout.view_video_player, this, true)
        setOnClickListener(this)
        btnActionBack.setOnClickListener(this)
        ivLock.setOnClickListener(this)
        viewControl.setOnClickListener(this)
        sbPlayerViewProgress.setOnSeekBarChangeListener(this)

        audioManager = ContextCompat.getSystemService(context, AudioManager::class.java)!!
    }

    //IJKplayer 部分
    fun initData() {
        surfaceView.holder.addCallback(callback)
    }

    fun setTitle(title: String) {
        tvTitle.text = title
    }

    fun setScaleType(scaleType: VideoScaleType) {
        this.scaleType = scaleType
    }

    fun setVideoPath(urlString: String) {
        player.dataSource = urlString
    }

    fun start(isStart: Boolean = true) {
        if (isStart) {
            player.prepareAsync()
            player.setOnPreparedListener(this)
            player.setOnVideoSizeChangedListener(this)
            player.setOnInfoListener { iMediaPlayer, what, arg2 ->
                when (what) {
                    IMediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                        setState(STATUS_LOADING)
                    }
                    IMediaPlayer.MEDIA_INFO_BUFFERING_END,
                    IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                        setState(STATUS_PLAYING)
                    }
                }
                false
            }
        }
        player.start()
        viewControl.play()
        setState(STATUS_LOADING)

    }

    private fun setState(state: Int) {
        VIDEO_STATUS = state
        when (state) {
            STATUS_LOADING -> {
                pbLoading.visibility = View.VISIBLE
                topLayout.visibility = View.GONE
                bottomLayout.visibility = View.GONE
                ivLock.visibility = View.GONE
                viewControl.visibility = View.GONE
                Log.e("video-status", "loading")
            }
            STATUS_PLAYING -> {
                pbLoading.visibility = View.GONE
                Log.e("video-status", "playing")
            }
        }
    }


    fun pause() {
        if (player.isPlaying) {
            player.pause()
            viewControl.pause()
        }
    }

    fun resume() {
        start(false)
    }

    fun isPlaying() = player.isPlaying
    fun isPrepare() = isPrepare

    fun destroy() {
        player.reset()
        player.release()
        removeCallbacks(mHideControllerRunnable)
        removeCallbacks(mShowControllerRunnable)
        removeCallbacks(mHideMessageRunnable)
        removeCallbacks(mShowMessageRunnable)
    }


    private val callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            player.setDisplay(holder)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {

        }

    }
    private var isPrepare: Boolean = false
    override fun onPrepared(mPlayer: IMediaPlayer?) {
        isPrepare = true
        tvPlayerViewPlayTime.text = conversionTime(0)
        tvPlayerViewTotalTime.text = conversionTime(player.duration)
        sbPlayerViewProgress.max = player.duration.toInt()
        postDelayed(mRefreshRunnable, (REFRESH_TIME / 2).toLong())
        player.setOnErrorListener { iMediaPlayer, i, i2 ->
            onError?.invoke(iMediaPlayer, i, i2)
            false
        }
        player.setOnCompletionListener {
            viewControl.pause()
            onCompletion?.invoke(it)
        }

        player.setOnBufferingUpdateListener { _, progress ->
            sbPlayerViewProgress.secondaryProgress = (progress * player.duration / 100).toInt()
        }
    }

    //  UI部分
    override fun onClick(mView: View) {
        when (mView) {
            this -> {
                removeCallbacks(mHideControllerRunnable)
                removeCallbacks(mShowControllerRunnable)
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
            ivLock -> {
                isLock = !isLock
                ivLock.setImageResource(if (isLock) R.drawable.icon_video_lock_close else R.drawable.icon_video_lock_open)
                topLayout.visibility = if (isLock) View.GONE else View.VISIBLE
                bottomLayout.visibility = if (isLock) View.GONE else View.VISIBLE
                viewControl.visibility = if (isLock) View.GONE else View.VISIBLE
            }
            viewControl -> {
                if (viewControl.visibility != VISIBLE) {
                    return
                }
                if (player.isPlaying) {
                    pause()
                } else {
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


    private fun hideControlPanel() {
        if (!isControlPanelShow) {
            return
        }
        isControlPanelShow = false
        val topAnimator: ValueAnimator = ofInt(0, -topLayout.height)
        topAnimator.duration = ANIM_TIME.toLong()
        topAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            topLayout.translationY = translationY.toFloat()
            if (translationY != -topLayout.height) {
                return@addUpdateListener
            }
            if (topLayout.visibility == INVISIBLE) {
                topLayout.visibility = VISIBLE
            }
        }
        topAnimator.start()
        val bottomAnimator: ValueAnimator = ofInt(0, bottomLayout.height)
        bottomAnimator.duration = ANIM_TIME.toLong()
        bottomAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            bottomLayout.translationY = translationY.toFloat()
            if (translationY != bottomLayout.height) {
                return@addUpdateListener
            }
            if (bottomLayout.visibility == INVISIBLE) {
                bottomLayout.visibility = VISIBLE
            }
        }
        bottomAnimator.start()
        val alphaAnimator: ValueAnimator = ValueAnimator.ofFloat(1f, 0f)
        alphaAnimator.duration = ANIM_TIME.toLong()
        alphaAnimator.addUpdateListener {
            val alpha: Float = it.animatedValue as Float
            ivLock.alpha = alpha
            viewControl.alpha = alpha
            if (alpha != 0f) {
                return@addUpdateListener
            }
            if (ivLock.visibility == INVISIBLE) {
                ivLock.visibility = VISIBLE
            }
            if (viewControl.visibility == VISIBLE) {
                viewControl.visibility = INVISIBLE
            }
        }
        alphaAnimator.start()
    }

    private fun showControlPanel() {
        if (isControlPanelShow || VIDEO_STATUS == STATUS_LOADING) {
            return
        }
        isControlPanelShow = true

        if (topLayout.visibility == View.GONE) {
            topLayout.visibility = View.VISIBLE
            bottomLayout.visibility = View.VISIBLE
            ivLock.visibility = View.VISIBLE
            viewControl.visibility = View.VISIBLE
        }


        val topAnimator: ValueAnimator = ofInt(-topLayout.height, 0)
        topAnimator.duration = ANIM_TIME.toLong()
        topAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            topLayout.translationY = translationY.toFloat()
            if (translationY != -topLayout.height) {
                return@addUpdateListener
            }
            if (topLayout.visibility == INVISIBLE) {
                topLayout.visibility = VISIBLE
            }
        }
        topAnimator.start()
        val bottomAnimator: ValueAnimator = ofInt(bottomLayout.height, 0)
        bottomAnimator.duration = ANIM_TIME.toLong()
        bottomAnimator.addUpdateListener {
            val translationY: Int = it.animatedValue as Int
            bottomLayout.translationY = translationY.toFloat()
            if (translationY != bottomLayout.height) {
                return@addUpdateListener
            }
            if (bottomLayout.visibility == INVISIBLE) {
                bottomLayout.visibility = VISIBLE
            }
        }
        bottomAnimator.start()
        val alphaAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f)
        alphaAnimator.duration = ANIM_TIME.toLong()
        alphaAnimator.addUpdateListener {
            val alpha: Float = it.animatedValue as Float
            ivLock.alpha = alpha
            viewControl.alpha = alpha
            if (alpha != 0f) {
                return@addUpdateListener
            }
            if (ivLock.visibility == INVISIBLE) {
                ivLock.visibility = VISIBLE
            }
            if (viewControl.visibility == INVISIBLE) {
                viewControl.visibility = VISIBLE
            }

        }
        alphaAnimator.start()

    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 满足任一条件：关闭手势控制、处于锁定状态、处于缓冲状态
        if (!gestureEnabled || isLock) {
            return super.onTouchEvent(event)
        }
        when (event.action) {
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
                removeCallbacks(mHideControllerRunnable)
            }
            MotionEvent.ACTION_MOVE -> run {
                // 计算偏移的距离（按下的位置 - 当前触摸的位置）
                val distanceX: Float = viewDownX - event.x
                val distanceY: Float = viewDownY - event.y
                // 手指偏移的距离一定不能太短，这个是前提条件
                if (abs(distanceY) < ViewConfiguration.get(context).scaledTouchSlop) {
                    return@run
                }
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
                    val second: Int = (-(distanceX / width.toFloat() * 60f)).toInt()
                    val progress: Int = getProgress() + second * 1000
                    if (progress >= 0 && progress <= getDuration()) {
                        adjustSecond = second
                        ivMessage.setImageResource(if (adjustSecond < 0) R.drawable.video_schedule_rewind_ic else R.drawable.video_schedule_forward_ic)
                        tvMessage.text = String.format("%s s", abs(adjustSecond))
                        post(mShowMessageRunnable)
                    }
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
                if (abs(viewDownX - event.x) <= ViewConfiguration.get(context).scaledTouchSlop &&
                    abs(viewDownY - event.y) <= ViewConfiguration.get(context).scaledTouchSlop
                ) {
                    // 如果整个视频播放区域太大，触摸移动会导致触发点击事件，所以这里换成手动派发点击事件
                    if (isEnabled && isClickable) {
                        performClick()
                    }
                }
                touchOrientation = -1
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (adjustSecond != 0) {
                    // 调整播放进度
                    setProgress(getProgress() + adjustSecond * 1000)
                    adjustSecond = 0
                }
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
                postDelayed(mHideMessageRunnable, DIALOG_TIME.toLong())
            }
            MotionEvent.ACTION_CANCEL -> {
                touchOrientation = -1
                currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (adjustSecond != 0) {
                    setProgress(getProgress() + adjustSecond * 1000)
                    adjustSecond = 0
                }
                postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
                postDelayed(mHideMessageRunnable, DIALOG_TIME.toLong())
            }
        }
        return true
    }

    private fun getDuration(): Int = player.duration.toInt()

    private fun getProgress(): Int = player.currentPosition.toInt()


    /** 返回事件回调 */
    var onActionBack: (() -> Unit)? = null

    /** 播放完成回调 */
    var onCompletion: ((IMediaPlayer) -> Unit)? = null

    var onError: ((IMediaPlayer, Int, Int) -> Unit)? = null

    /**
     * 时间转换
     */
    private fun conversionTime(time: Long): String {
        val formatter = Formatter(Locale.getDefault())
        // 总秒数
        val totalSeconds: Long = time / 1000
        // 小时数
        val hours: Long = totalSeconds / 3600
        // 分钟数
        val minutes: Long = (totalSeconds / 60) % 60
        // 秒数
        val seconds: Long = totalSeconds % 60
        return if (hours > 0) {
            formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            formatter.format("%02d:%02d", minutes, seconds).toString()
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
        removeCallbacks(mRefreshRunnable)
        removeCallbacks(mHideControllerRunnable)
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
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
            val progress = player.currentPosition
            tvPlayerViewPlayTime.text = conversionTime(progress)
            sbPlayerViewProgress.progress = progress.toInt()
            postDelayed(this, REFRESH_TIME.toLong())

        }
    }

    /**
     * 设置视频播放进度
     */
    private fun setProgress(progress: Int) {
        var finalProgress: Int = progress
        if (finalProgress > player.duration) {
            finalProgress = player.duration.toInt()
        }
        player.seekTo(finalProgress.toLong())
        sbPlayerViewProgress.progress = finalProgress
        if (!player.isPlaying) start(false)
    }

    override fun onVideoSizeChanged(
        p0: IMediaPlayer?,
        videoWidth: Int,
        videoHeight: Int,
        p3: Int,
        p4: Int
    ) {
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
                val width = height * videoWidth / videoHeight
                val layoutPrams = LayoutParams(width, height)
                layoutPrams.gravity = 0x11
                surfaceView.layoutParams = layoutPrams
            }
        }

    }


}