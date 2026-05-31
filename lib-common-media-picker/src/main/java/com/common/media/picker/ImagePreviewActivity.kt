package com.common.media.picker

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.common.media.picker.databinding.ActivityImagePreviewBinding
import com.common.ui.BaseAppBindActivity
import com.common.weight.titlebar.CommonTitleBar
import com.common.weight.titlebar.ScreenUtils
import java.io.File
import java.util.ArrayList
import java.util.Locale

class ImagePreviewActivity : BaseAppBindActivity<ActivityImagePreviewBinding>() {

    companion object {
        fun start(mContext: Context, dataList: ArrayList<String>, position: Int) {
            val intent = Intent()
            intent.setClass(mContext, ImagePreviewActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("dataList", dataList)
            mContext.startActivity(intent)
        }

        /** 进度条刻度（取分数避免长视频 Int 溢出） */
        private const val SEEK_BAR_MAX = 1000
        private const val PROGRESS_INTERVAL = 500L
    }

    private var mAdapter: ImagePreviewAdapter? = null

    /** 全局唯一播放器，绑定到当前可见的视频页 */
    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply { addListener(playerListener) }
    }

    /** 当前已绑定播放器的视频 ViewHolder */
    private var boundHolder: ImagePreviewAdapter.VideoViewHolder? = null

    /** 用户正在拖动进度条时，暂停自动刷新进度 */
    private var isUserSeeking = false

    private val progressHandler = Handler(Looper.getMainLooper())

    override fun getLayoutId(): Int = R.layout.activity_image_preview

    override fun initialize(savedInstanceState: Bundle?) {
        val position = intent.getIntExtra("position", 0)
        mPosition = position
        val dataList = intent.getStringArrayListExtra("dataList")
        binding.viewPager.offscreenPageLimit = 3
        mAdapter = ImagePreviewAdapter(this@ImagePreviewActivity, dataList!!)
        binding.viewPager.adapter = mAdapter
        binding.viewPager.setCurrentItem(position, false)
        binding.viewPager.registerOnPageChangeCallback(mPageChangeCallback)
        getTitleBarView()?.setCenterText("${(position + 1)}/${mAdapter?.itemCount}")

        getTitleBarView()?.setRightText("全屏", Color.parseColor("#FF40A9FF"), ScreenUtils.dp2PxInt(this, 16f))
        getTitleBarView()?.setListener {  _, action, _ ->
            when (action) {
                CommonTitleBar.ACTION_RIGHT_TEXT ->{
                    openVideoPlayerByReflect(this@ImagePreviewActivity,dataList[mPosition])
                }
                CommonTitleBar.ACTION_LEFT_BUTTON ->{
                    onBackPressedDispatcher.onBackPressed()
                }


            }
        }

        // 首次进入：等布局完成后播放当前页（若为视频）
        binding.viewPager.post { playCurrentIfVideo(position) }
    }
    private var mPosition:Int =0
    private val mPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            mPosition = position
            getTitleBarView()?.setCenterText("${(position + 1)}/${mAdapter?.itemCount}")
            // 滑走时暂停并解绑上一个视频，再尝试播放当前页
            detachCurrent()
            playCurrentIfVideo(position)
        }
    }

    /** ViewPager2 内部的 RecyclerView */
    private val recyclerView: RecyclerView?
        get() = binding.viewPager.getChildAt(0) as? RecyclerView

    /** 若当前页是视频，则绑定播放器并播放（ViewHolder 可能尚未 attach，做有限次重试） */
    private fun playCurrentIfVideo(position: Int, retry: Int = 0) {
        val adapter = mAdapter ?: return
        if (position !in 0 until adapter.itemCount) return
        if (!adapter.isVideo(position)) return
        val rv = recyclerView
        val holder = rv?.findViewHolderForAdapterPosition(position)
                as? ImagePreviewAdapter.VideoViewHolder
        if (holder == null) {
            if (retry < 5) rv?.post { playCurrentIfVideo(position, retry + 1) }
            return
        }
        bindAndPlay(holder, position)
    }

    private fun bindAndPlay(holder: ImagePreviewAdapter.VideoViewHolder, position: Int) {
        boundHolder = holder
        holder.binding.playerView.player = player
        holder.binding.ivPlayPause.setOnClickListener { togglePlay() }
        setupSeekBar(holder)

        val path = mAdapter?.getPath(position) ?: return
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
        player.seekTo(0)
        player.playWhenReady = true
        player.prepare()
        startProgress()
    }

    /** 暂停并解绑当前视频页 */
    private fun detachCurrent() {
        stopProgress()
        boundHolder?.let {
            it.binding.playerView.player = null
            it.binding.ivPlayPause.setImageResource(R.drawable.icon_video_play)
        }
        if (player.isPlaying) player.pause()
        boundHolder = null
    }

    private fun togglePlay() {
        if (player.isPlaying) {
            player.pause()
        } else {
            // 播放结束后再次点击从头播放
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        boundHolder?.binding?.ivPlayPause?.setImageResource(
            if (isPlaying) R.drawable.icon_video_pause else R.drawable.icon_video_play
        )
    }

    private fun setupSeekBar(holder: ImagePreviewAdapter.VideoViewHolder) {
        holder.binding.seekBar.max = SEEK_BAR_MAX
        holder.binding.seekBar.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player.duration
                    if (duration > 0) {
                        holder.binding.tvPosition.text =
                            formatTime(duration * progress / SEEK_BAR_MAX)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val duration = player.duration
                if (duration > 0) {
                    player.seekTo(duration * seekBar.progress / SEEK_BAR_MAX)
                }
                isUserSeeking = false
            }
        })
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            val holder = boundHolder
            if (holder != null) {
                val duration = player.duration
                val pos = player.currentPosition.coerceAtLeast(0)
                if (duration > 0) {
                    if (!isUserSeeking) {
                        holder.binding.seekBar.progress = (pos * SEEK_BAR_MAX / duration).toInt()
                    }
                    holder.binding.tvDuration.text = formatTime(duration)
                }
                holder.binding.tvPosition.text = formatTime(pos)
            }
            progressHandler.postDelayed(this, PROGRESS_INTERVAL)
        }
    }

    private fun startProgress() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }

    private fun stopProgress() {
        progressHandler.removeCallbacks(progressRunnable)
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlayPauseIcon(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                updatePlayPauseIcon(false)
                boundHolder?.binding?.seekBar?.progress = SEEK_BAR_MAX
            }
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (player.isPlaying) player.pause()
    }

    override fun onResume() {
        super.onResume()
        // 回到前台且当前为视频页时继续播放
        if (boundHolder != null && player.playbackState != Player.STATE_IDLE) {
            player.play()
        }
    }

    override fun onDestroy() {
        // 必须在 super.onDestroy() 之前访问 binding（基类会在 onDestroy 中置空 binding）
        binding.viewPager.unregisterOnPageChangeCallback(mPageChangeCallback)
        stopProgress()
        player.release()
        super.onDestroy()
    }

    fun openVideoPlayerByReflect(context: Context, url: String) {
        try {
            val clazz = Class.forName("com.demo.project.ui.activity.VideoPlayerActivity")

            val method = clazz.getDeclaredMethod(
                "start",
                Context::class.java,
                String::class.java
            )

            method.isAccessible = true

            // 因为 start 是 @JvmStatic，所以第一个参数传 null
            method.invoke(null, context, url)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
