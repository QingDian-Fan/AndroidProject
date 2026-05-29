package com.demo.project.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.common.ui.BaseAppBindActivity
import com.common.utils.StatusBarUtil
import com.common.weight.video.VideoScaleType
import com.demo.project.databinding.ActivityVideoPlayerBinding
import com.demo.project.utils.ext.gone

class VideoPlayerActivity: BaseAppBindActivity<ActivityVideoPlayerBinding>() {
    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, VideoPlayerActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup
    ): ActivityVideoPlayerBinding = ActivityVideoPlayerBinding.inflate(inflater, container, false)

    override fun initialize(savedInstanceState: Bundle?) {
        // 强制横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        getTitleBarView()?.visibility = gone
        StatusBarUtil.hideStatusBar(this)
        binding.videoView.initData()
        binding.videoView.setVideoPath("https://media.w3.org/2010/05/sintel/trailer.mp4")
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