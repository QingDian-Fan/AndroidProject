package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.SurfaceHolder
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityVideoPlayerBinding
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import java.io.IOException


class VideoPlayerActivity : BaseAppBindActivity<ActivityVideoPlayerBinding>() {


    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, VideoPlayerActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    private val player by lazy {
        IjkMediaPlayer()
    }

    override fun getLayoutId(): Int = R.layout.activity_video_player

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().visibility = gone
        binding.surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                player.setDisplay(holder)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {

            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {

            }

        })

        try {
            player.dataSource = "http://vfx.mtime.cn/Video/2019/03/09/mp4/190309153658147087.mp4";
            player.prepareAsync()
            player.start()
        } catch (e: IOException) {
            e.printStackTrace();
        }

    }

    override fun onPause() {
        super.onPause()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.reset()
        player.release()
    }


}