package com.demo.project.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import com.common.ui.BaseAppBindActivity
import com.common.utils.StatusBarUtil
import com.common.weight.video.VideoScaleType
import com.demo.project.R
import com.demo.project.databinding.ActivityVideoPlayerBinding
import com.demo.project.utils.ext.gone

class VideoPlayerActivity: BaseAppBindActivity<ActivityVideoPlayerBinding>() {
    companion object {
        const val KEY_VIDEO_URL_STRING="KEY_VIDEO_URL_STRING"
        @JvmStatic
        fun start(mContext: Context,urlString: String) {
            val intent = Intent()
            intent.setClass(mContext, VideoPlayerActivity::class.java)
            intent.putExtra(KEY_VIDEO_URL_STRING,urlString)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_video_player

    override fun initialize(savedInstanceState: Bundle?) {
        // 强制横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        getTitleBarView()?.visibility = gone
        var videoUrlString = intent.getStringExtra(KEY_VIDEO_URL_STRING)
        StatusBarUtil.hideStatusBar(this)
        binding.videoView.initData()
        videoUrlString?:run {
            videoUrlString = "https://media.w3.org/2010/05/sintel/trailer.mp4"
        }
        videoUrlString?.let {
            binding.videoView.setVideoPath(it)
        }
        binding.videoView.setScaleType(VideoScaleType.RATIO_FILL_SIZE)
        binding.videoView.setSpeed(1f)
        binding.videoView.start()

        binding.videoView.onActionBack = {
            backPress(null)
        }
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