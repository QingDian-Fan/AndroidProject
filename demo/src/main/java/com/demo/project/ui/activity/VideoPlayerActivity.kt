package com.demo.project.ui.activity

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import com.common.ui.BaseAppBindActivity
import com.common.utils.LogUtil
import com.common.utils.StatusBarUtil
import com.common.utils.ToastUtil
import com.common.weight.video.VideoScaleType
import com.demo.project.R
import com.demo.project.databinding.ActivityVideoPlayerBinding
import com.demo.project.player.CommonPlayerVideoEngine
import com.demo.project.utils.ext.gone

class VideoPlayerActivity: BaseAppBindActivity<ActivityVideoPlayerBinding>() {
    companion object {
        private const val TAG = "VideoPlayerActivity"

        const val KEY_VIDEO_URL_STRING="KEY_VIDEO_URL_STRING"

        /** 退化判断：常见音频文件后缀（mimeType 不可用时使用） */
        private val AUDIO_EXTENSIONS = listOf(
            ".mp3", ".aac", ".wav", ".flac", ".ogg", ".m4a", ".amr", ".wma", ".opus", ".mid", ".ape"
        )

        /**
         * 方向切换兜底超时：系统在极端情况下（例如目标方向与当前方向已经一致、
         * 或窗口未真正发生配置变化）可能不回调 [onConfigurationChanged]，
         * 超时后按真实方向重新同步状态，避免方向按钮永久卡在“切换中”。
         */
        private const val ORIENTATION_SWITCH_TIMEOUT_MS = 1500L

        @JvmStatic
        fun start(mContext: Context,urlString: String?=null) {
            val intent = Intent()
            intent.setClass(mContext, VideoPlayerActivity::class.java).apply {
                if (mContext !is Activity) {
                    flags=FLAG_ACTIVITY_NEW_TASK
                }
            }
            intent.putExtra(KEY_VIDEO_URL_STRING,urlString)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_video_player

    /** 当前是否为视频（音频不纳入本次横竖屏切换功能） */
    private var isVideoMedia: Boolean = false

    /** 当前方向，取值为 [Configuration.ORIENTATION_LANDSCAPE] / [Configuration.ORIENTATION_PORTRAIT] */
    private var currentOrientation: Int = Configuration.ORIENTATION_LANDSCAPE

    /** 手动切换的目标方向；未在切换中时与 [currentOrientation] 一致 */
    private var targetOrientation: Int = Configuration.ORIENTATION_LANDSCAPE

    /** 是否正在等待系统把配置切换到 [targetOrientation] */
    private var isSwitchingOrientation: Boolean = false

    private val orientationTimeoutRunnable = Runnable { syncOrientationState(true) }

    /** 进入后台前是否处于播放中：用于区分用户主动暂停与页面切后台导致的暂停 */
    private var shouldResumeOnForeground: Boolean = false

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.visibility = gone
        // 播放期间保持屏幕常亮（页面销毁时随窗口自动清除）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 全屏沉浸：隐藏状态栏 + 底部导航栏（手势 home 条）
        StatusBarUtil.hideSystemBars(this)

        // 解析播放地址：外部 App 通过 ACTION_VIEW 调起时走 intent.data，内部调用走 extra
        val mediaUri = resolveMediaUri()
        val isAudio = isAudioMedia(mediaUri)

        isVideoMedia = !isAudio

        // 音频不强制横屏（跟随系统/竖屏），视频首次进入维持默认横屏
        requestedOrientation = if (isAudio) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        currentOrientation = if (isAudio) {
            resources.configuration.orientation
        } else {
            Configuration.ORIENTATION_LANDSCAPE
        }
        targetOrientation = currentOrientation

        // 正式入口不做静默兜底：地址为空直接提示并结束，测试地址只保留在演示入口
        if (mediaUri.isNullOrBlank()) {
            ToastUtil.showToast(this, getString(R.string.toast_video_url_invalid))
            finish()
            return
        }
        // content:// 需要 ContentResolver 授权读取，FFmpeg 无法直接打开，
        // 且必须先确认授权仍然有效，否则同样按地址无效处理
        val contentUri = isContentUri(mediaUri)
        if (contentUri && !canOpenContentUri(mediaUri)) {
            ToastUtil.showToast(this, getString(R.string.toast_video_url_invalid))
            finish()
            return
        }

        // 引擎注入必须早于 initData() 与 setVideoPath()。
        // content:// 路由到 ExoPlayer（内部使用 ContentResolver），其余视频使用 FFmpeg 引擎。
        if (!isAudio && !contentUri) {
            binding.videoView.setPlayerEngineFactory { context -> CommonPlayerVideoEngine(context.applicationContext) }
        }
        binding.videoView.initData()
        binding.videoView.setVideoPath(mediaUri)
        binding.videoView.setScaleType(VideoScaleType.RATIO_FILL_SIZE)
        binding.videoView.setSpeed(1f)
        binding.videoView.start()

        // 音频不纳入横竖屏切换功能，直接隐藏方向入口，避免出现无响应的可点击控件
        binding.videoView.setOrientationSwitchEnabled(isVideoMedia)
        binding.videoView.updateOrientation(currentOrientation == Configuration.ORIENTATION_LANDSCAPE)

        binding.videoView.onActionBack = {
            backPress(null)
        }
        binding.videoView.onOrientationSwitch = {
            switchOrientation()
        }
        binding.videoView.onSpeedChangeFailed = {
            ToastUtil.showToast(this, getString(R.string.toast_video_speed_change_failed))
        }
    }

    /**
     * 手动切换横竖屏。播放器实例、数据源和播放进度完全复用，
     * 页面通过 Manifest 的 configChanges 自行处理配置变化，不会重建 Activity。
     */
    private fun switchOrientation() {
        if (!isVideoMedia || isSwitchingOrientation) {
            // 上一次切换尚未完成时忽略后续点击，避免来回抖动
            return
        }
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        targetOrientation = if (landscape) {
            Configuration.ORIENTATION_PORTRAIT
        } else {
            Configuration.ORIENTATION_LANDSCAPE
        }
        isSwitchingOrientation = true
        // 固定到具体方向而不是 UNSPECIFIED/SENSOR，避免传感器在切换后立即把画面转回去
        requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        window.decorView.removeCallbacks(orientationTimeoutRunnable)
        window.decorView.postDelayed(orientationTimeoutRunnable, ORIENTATION_SWITCH_TIMEOUT_MS)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        syncOrientationState(false)
        // 配置变化会重新显示系统栏，这里恢复沉浸式播放
        StatusBarUtil.hideSystemBars(this)
    }

    /**
     * 以系统实际配置为准同步方向状态。
     * 只有配置确实变成目标方向后才清除“切换中”标记并更新按钮图标；
     * [byTimeout] 为 true 时说明系统未按预期回调，按真实方向兜底重置，避免永久卡在切换中。
     */
    private fun syncOrientationState(byTimeout: Boolean) {
        val orientation = resources.configuration.orientation
        if (isSwitchingOrientation && orientation != targetOrientation && !byTimeout) {
            // 目标方向尚未生效：忽略这次中间态配置变化，不更新按钮，也不允许反向请求
            return
        }
        window.decorView.removeCallbacks(orientationTimeoutRunnable)
        isSwitchingOrientation = false
        currentOrientation = orientation
        targetOrientation = orientation
        binding.videoView.updateOrientation(orientation == Configuration.ORIENTATION_LANDSCAPE)
    }

    /**
     * 取播放地址：优先取外部 App 通过 [Intent.ACTION_VIEW] 传入的 data（content:// / file:// / http(s)://），
     * 其次取内部调用通过 [KEY_VIDEO_URL_STRING] 传入的字符串地址。
     */
    private fun resolveMediaUri(): String? {
        if (Intent.ACTION_VIEW == intent.action) {
            intent.data?.let { return it.toString() }
        }
        return intent.getStringExtra(KEY_VIDEO_URL_STRING)
    }

    /**
     * 判断当前媒体是否为音频：优先用 intent 携带的 mimeType，其次用 ContentResolver 解析的类型，
     * 最后退化到按文件扩展名判断。
     *
     * 查询 ContentProvider 可能因授权失效抛 [SecurityException]，也可能因 Provider 不存在抛
     * 其他异常，这里统一按「类型未知」处理并继续走扩展名判断，不能让页面直接崩溃。
     */
    private fun isAudioMedia(uri: String?): Boolean {
        intent.type?.let {
            if (it.startsWith("audio/")) return true
            if (it.startsWith("video/")) return false
        }
        intent.data?.let { data ->
            runCatching { contentResolver.getType(data) }.getOrNull()
        }?.let {
            if (it.startsWith("audio/")) return true
            if (it.startsWith("video/")) return false
        }
        val path = uri?.substringBefore('?')?.lowercase() ?: return false
        return AUDIO_EXTENSIONS.any { path.endsWith(it) }
    }

    private fun isContentUri(uri: String): Boolean =
        ContentResolver.SCHEME_CONTENT.equals(Uri.parse(uri).scheme, ignoreCase = true)

    /**
     * 打开一次 content:// 描述符以确认授权仍然有效且文件存在。
     * 立即关闭，播放期间由 ExoPlayer 自行通过 ContentResolver 重新打开并持有描述符。
     *
     * 授权失效（[SecurityException]）、文件不存在（[FileNotFoundException]）以及
     * Provider 抛出的其他异常都视为不可播放。
     */
    private fun canOpenContentUri(uri: String): Boolean = runCatching {
        contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    }.getOrElse { error ->
        // 不打印完整地址，避免把带鉴权参数的 URI 写进日志
        LogUtil.e(TAG, "open content uri failed: ${error.javaClass.simpleName}")
        false
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 重新获得焦点时再次隐藏，避免上滑唤出后系统栏不再消失
        if (hasFocus) StatusBarUtil.hideSystemBars(this)
    }

    override fun onPause() {
        super.onPause()
        // 必须在 pause() 之前采样，且必须取「用户期望的播放状态」而不是瞬时 isPlaying：
        // 缓冲中或 Surface 尚未创建时 isPlaying 为 false，但用户仍期望继续播放。
        // 用户主动点击暂停时该值为 false，返回前台必须保持暂停。
        shouldResumeOnForeground = binding.videoView.isPlayIntended()
        binding.videoView.pause()
    }

    override fun onResume() {
        super.onResume()
        if (shouldResumeOnForeground &&
            !binding.videoView.isPlaying() &&
            binding.videoView.isPrepare()
        ) {
            binding.videoView.resume()
        }
        shouldResumeOnForeground = false
    }

    override fun onDestroy() {
        // 释放播放器需在 super.onDestroy() 之前，否则基类会先把 binding 置空导致访问报错
        window.decorView.removeCallbacks(orientationTimeoutRunnable)
        binding.videoView.destroy()
        super.onDestroy()
    }
}
