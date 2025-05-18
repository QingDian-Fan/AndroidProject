package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityVideoPlayerBinding
import com.dian.demo.ui.view.video.VideoScaleType
import com.dian.demo.utils.StatusBarUtil


class VideoPlayerActivity : BaseAppBindActivity<ActivityVideoPlayerBinding>() {


    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, VideoPlayerActivity::class.java)
            mContext.startActivity(intent)
        }
    }


    override fun getLayoutId(): Int = R.layout.activity_video_player

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().visibility = gone
        StatusBarUtil.hideStatusBar(this)
        binding.videoView.initData()
        binding.videoView.setVideoPath("http://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4")
        binding.videoView.setScaleType(VideoScaleType.RATIO_FILL_SIZE)
        binding.videoView.setSpeed(3f)
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
        super.onDestroy()
        binding.videoView.destroy()
    }


}