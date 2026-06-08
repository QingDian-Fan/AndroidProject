package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import com.common.ui.BaseAppBindActivity
import com.common.utils.StatusBarUtil
import com.common.weight.video.VideoScaleType
import com.demo.project.R
import com.demo.project.databinding.ActivityVideoPlayerBinding
import com.demo.project.player.CommonPlayerVideoEngine
import com.demo.project.utils.ext.gone

class VideoPlayerActivity: BaseAppBindActivity<ActivityVideoPlayerBinding>() {
    companion object {
        const val KEY_VIDEO_URL_STRING="KEY_VIDEO_URL_STRING"

        /** 退化判断：常见音频文件后缀（mimeType 不可用时使用） */
        private val AUDIO_EXTENSIONS = listOf(
            ".mp3", ".aac", ".wav", ".flac", ".ogg", ".m4a", ".amr", ".wma", ".opus", ".mid", ".ape"
        )

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

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.visibility = gone
        // 播放期间保持屏幕常亮（页面销毁时随窗口自动清除）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 全屏沉浸：隐藏状态栏 + 底部导航栏（手势 home 条）
        StatusBarUtil.hideSystemBars(this)

        // 解析播放地址：外部 App 通过 ACTION_VIEW 调起时走 intent.data，内部调用走 extra
        val mediaUri = resolveMediaUri()
        val isAudio = isAudioMedia(mediaUri)

        // 音频不强制横屏（跟随系统/竖屏），视频维持强制横屏
        requestedOrientation = if (isAudio) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        if (!isAudio) {
            binding.videoView.setPlayerEngineFactory { context -> CommonPlayerVideoEngine(context.applicationContext) }
        }
        binding.videoView.initData()
        val playUrl = mediaUri ?: "rtmp://ns8.indexforce.com/home/mystream"
        binding.videoView.setVideoPath(playUrl)
        binding.videoView.setScaleType(VideoScaleType.RATIO_FILL_SIZE)
        binding.videoView.setSpeed(1f)
        binding.videoView.start()

        binding.videoView.onActionBack = {
            backPress(null)
        }
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
     */
    private fun isAudioMedia(uri: String?): Boolean {
        intent.type?.let {
            if (it.startsWith("audio/")) return true
            if (it.startsWith("video/")) return false
        }
        intent.data?.let { contentResolver.getType(it) }?.let {
            if (it.startsWith("audio/")) return true
            if (it.startsWith("video/")) return false
        }
        val path = uri?.substringBefore('?')?.lowercase() ?: return false
        return AUDIO_EXTENSIONS.any { path.endsWith(it) }
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 重新获得焦点时再次隐藏，避免上滑唤出后系统栏不再消失
        if (hasFocus) StatusBarUtil.hideSystemBars(this)
    }

    override fun onPause() {
        super.onPause()
        binding.videoView.pause()
    }

    override fun onResume() {
        super.onResume()
        if (!binding.videoView.isPlaying() && binding.videoView.isPrepare()) {
            binding.videoView.resume()
        }

    }

    override fun onDestroy() {
        // 释放播放器需在 super.onDestroy() 之前，否则基类会先把 binding 置空导致访问报错
        binding.videoView.destroy()
        super.onDestroy()
    }
}
