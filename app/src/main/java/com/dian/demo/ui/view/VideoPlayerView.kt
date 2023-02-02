package com.dian.demo.ui.view

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.airbnb.lottie.LottieAnimationView
import com.dian.demo.R

class VideoPlayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes), LifecycleEventObserver,
    SeekBar.OnSeekBarChangeListener, View.OnClickListener {

    private val topLayout: ViewGroup
    private val titleView: TextView
    private val leftView: View
    private val bottomLayout: ViewGroup
    private val playTime: TextView
    private val totalTime: TextView
    private val progressView: SeekBar
    private val videoView: VideoView
    private val controlView: VideoPlayButton
    private val lockView: ImageView
    private val messageLayout: ViewGroup
    private val lottieView: LottieAnimationView
    private val messageView: TextView

    init {
        LayoutInflater.from(getContext()).inflate(R.layout.view_video_player, this, true)
        topLayout = findViewById(R.id.ll_player_view_top)
        leftView = findViewById(R.id.iv_player_view_left)
        titleView = findViewById(R.id.tv_player_view_title)
        bottomLayout = findViewById(R.id.ll_player_view_bottom)
        playTime = findViewById(R.id.tv_player_view_play_time)
        totalTime = findViewById(R.id.tv_player_view_total_time)
        progressView = findViewById(R.id.sb_player_view_progress)
        videoView = findViewById(R.id.vv_player_view_video)
        lockView = findViewById(R.id.iv_player_view_lock)
        controlView = findViewById(R.id.iv_player_view_control)
        messageLayout = findViewById(R.id.cv_player_view_message)
        lottieView = findViewById(R.id.lav_player_view_lottie)
        messageView = findViewById(R.id.tv_player_view_message)
        leftView.setOnClickListener(this)
        controlView.setOnClickListener(this)
        lockView.setOnClickListener(this)
        setOnClickListener(this)
        progressView.setOnSeekBarChangeListener(this)


    }



    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {

    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {

    }

    override fun onClick(v: View?) {

    }

}