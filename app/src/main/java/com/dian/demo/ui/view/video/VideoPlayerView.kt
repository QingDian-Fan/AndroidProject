package com.dian.demo.ui.view.video

import android.animation.ValueAnimator
import android.animation.ValueAnimator.*
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.appcompat.widget.AppCompatTextView
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.aop.SingleClick
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt


class VideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), OnClickListener,
    IMediaPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener {

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


    /** 是否锁定 */
    private var isLock = false

    /** 控制面板展示 */
    private var isControlPanelShow = false

    /** 动画执行时间 */
    private val ANIM_TIME: Int = 500

    /** 面板隐藏间隔 */
    private val CONTROLLER_TIME: Int = 3000

    /** 当前播放进度 */
    private var currentProgress: Int = 0

    /** 刷新间隔 */
    private val REFRESH_TIME: Int = 1000


    init {
        LayoutInflater.from(getContext()).inflate(R.layout.view_video_player, this, true)
        setOnClickListener(this)
        btnActionBack.setOnClickListener(this)
        ivLock.setOnClickListener(this)
        viewControl.setOnClickListener(this)
        sbPlayerViewProgress.setOnSeekBarChangeListener(this)
    }

    //IJKplayer 部分
    fun initData() {
        surfaceView.holder.addCallback(callback)
        post(mShowControllerRunnable)
    }

    fun setTitle(title: String) {
        tvTitle.text = title
    }

    fun setVideoPath(urlString: String) {
        player.dataSource = urlString
    }

    fun start(isStart: Boolean = true) {
        if (isStart) {
            player.prepareAsync()
        }
        player.start()
        player.setOnPreparedListener(this)
        viewControl.play()
        // 延迟隐藏控制面板
        removeCallbacks { hideControlPanel() }
        postDelayed(mHideControllerRunnable, CONTROLLER_TIME.toLong())
    }


    fun pause() {
        if (player.isPlaying) {
            player.pause()
            viewControl.pause()
        }
    }

    fun destroy() {
        player.reset()
        player.release()
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

    override fun onPrepared(mPlayer: IMediaPlayer?) {
        tvPlayerViewPlayTime.text = conversionTime(0)
        tvPlayerViewTotalTime.text = conversionTime(player.duration)
        sbPlayerViewProgress.max = player.duration.toInt()
        postDelayed(mRefreshRunnable, (REFRESH_TIME / 2).toLong())
    }

    //  UI部分
    @SingleClick
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
        if (isControlPanelShow) {
            return
        }
        isControlPanelShow = true

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


    /** 返回事件回调 */
    var onActionBack: (() -> Unit)? = null

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
            var progress: Int = player.currentPosition.toInt()
            // 这里优化了播放的秒数计算，将 800 毫秒估算成 1 秒
            if ((progress.plus(1000)) < player.duration) {
                // 进行四舍五入计算
                progress = (progress / 1000f).roundToInt() * 1000
            }
            tvPlayerViewPlayTime.text = conversionTime(progress.toLong())
            sbPlayerViewProgress.progress = progress
            //sbPlayerViewProgress.secondaryProgress = (player.bufferPercentage / 100f * videoView.duration).toInt()
            if (player.isPlaying) {
                if (!isLock && bottomLayout.visibility == GONE) {
                    bottomLayout.visibility = VISIBLE
                }
            } else {
                if (bottomLayout.visibility == VISIBLE) {
                    bottomLayout.visibility = GONE
                }
            }
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
        // 要跳转的进度必须和当前播放进度相差 1 秒以上
        if (abs(finalProgress - player.currentPosition) > 1000) {
            player.seekTo(finalProgress.toLong())
            sbPlayerViewProgress.progress = finalProgress
        }
    }


}