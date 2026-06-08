package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.SeekBar
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.common.ui.BaseAppBindActivity
import com.demo.project.R
import com.demo.project.databinding.ActivityAudioPlayerBinding
import com.demo.project.player.audio.AudioEngineType
import com.demo.project.player.audio.AudioPlayerEngine
import com.demo.project.player.audio.AudioPlayerEngines
import java.util.Locale
import kotlin.math.max

/**
 * 音频播放页。
 *
 * 播放内核默认使用 ExoPlayer（Media3），通过 [AudioPlayerEngines] 工厂创建，
 * 若需切换/拓展其它内核，只需注册新的 [com.demo.project.player.audio.AudioPlayerEngineFactory] 即可，
 * 本页代码无需改动。
 */
class AudioPlayerActivity : BaseAppBindActivity<ActivityAudioPlayerBinding>() {

    companion object {
        const val KEY_AUDIO_URL_STRING = "KEY_AUDIO_URL_STRING"
        const val KEY_AUDIO_TITLE_STRING = "KEY_AUDIO_TITLE_STRING"
        const val KEY_AUDIO_ENGINE_NAME = "KEY_AUDIO_ENGINE_NAME"

        /** 快进/快退步长（毫秒） */
        private const val SEEK_STEP_MS = 15_000L

        /** 进度刷新间隔（毫秒） */
        private const val PROGRESS_INTERVAL_MS = 500L

        /** 演示用默认音频地址 */
        private const val DEFAULT_AUDIO_URL =
            "https://fangtian-education.oss-cn-beijing.aliyuncs.com/im/teacher/test/2026/06/03/2056909715238363138_1780477077969.m4a"

        /**
         * @param engineType 指定播放内核，传 null 使用 [AudioPlayerEngines.defaultType]（默认 ExoPlayer）。
         *                   传 [AudioEngineType.FFMPEG] 即可切换到 lib_common-player 的 FFmpeg 内核。
         */
        @JvmStatic
        fun start(
            mContext: Context,
            urlString: String? = null,
            titleString: String? = null,
            engineType: AudioEngineType? = null,
        ) {
            val intent = Intent()
            intent.setClass(mContext, AudioPlayerActivity::class.java).apply {
                if (mContext !is Activity) {
                    flags = FLAG_ACTIVITY_NEW_TASK
                }
            }
            intent.putExtra(KEY_AUDIO_URL_STRING, urlString)
            intent.putExtra(KEY_AUDIO_TITLE_STRING, titleString)
            intent.putExtra(KEY_AUDIO_ENGINE_NAME, engineType?.name)
            mContext.startActivity(intent)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: AudioPlayerEngine? = null

    /** 用户拖动进度条期间暂停自动刷新，避免回跳 */
    private var isUserSeeking = false

    private val progressTicker = object : Runnable {
        override fun run() {
            updateProgress()
            mainHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_audio_player

    override fun initialize(savedInstanceState: Bundle?) {
        hideNavigationBar()
        val title = resolveTitle()
        setPageTitle(title)
        binding.tvTitle.text = title
        binding.tvSubtitle.setText(R.string.audio_state_preparing)

        setupControls()
        setupEngine(resolveMediaUri() ?: DEFAULT_AUDIO_URL, resolveEngineType())
    }

    /**
     * 隐藏底部虚拟导航栏（仅本页生效，状态栏保留）。
     * 用户上滑可临时唤出，失焦/重新获得焦点后会再次隐藏（见 [onWindowFocusChanged]）。
     */
    private fun hideNavigationBar() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    /** 创建并启动播放引擎（默认 ExoPlayer，可通过工厂切换到 FFmpeg 等内核） */
    private fun setupEngine(url: String, engineType: AudioEngineType) {
        val player = AudioPlayerEngines.create(this, engineType)
        engine = player
        player.setListener(object : AudioPlayerEngine.Listener {
            override fun onBuffering() {
                binding.tvSubtitle.setText(R.string.audio_state_buffering)
            }

            override fun onReady() {
                binding.tvDuration.text = formatTime(player.duration)
                binding.sbProgress.max = max(player.duration.toInt(), 0)
                updateSubtitle(player.isPlaying)
            }

            override fun onPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseIcon(isPlaying)
                updateSubtitle(isPlaying)
            }

            override fun onEnded() {
                binding.tvSubtitle.setText(R.string.audio_state_completed)
                updatePlayPauseIcon(false)
            }

            override fun onError(error: Throwable) {
                binding.tvSubtitle.text =
                    getString(R.string.audio_state_error, error.message ?: "")
                updatePlayPauseIcon(false)
            }
        })
        player.setDataSource(url)
        player.prepare()
        player.play()
        startProgressTicker()
    }

    private fun setupControls() {
        binding.ivPlayPause.setOnClickListener { togglePlayPause() }
        binding.ivRewind.setOnClickListener { seekBy(-SEEK_STEP_MS) }
        binding.ivForward.setOnClickListener { seekBy(SEEK_STEP_MS) }

        binding.sbProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrent.text = formatTime(progress.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
                engine?.seekTo(seekBar.progress.toLong())
            }
        })
    }

    private fun togglePlayPause() {
        val player = engine ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            // 播放结束后再次点击，从头开始
            if (player.isEnded) player.seekTo(0)
            player.play()
        }
    }

    private fun seekBy(deltaMs: Long) {
        val player = engine ?: return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, player.duration)
        player.seekTo(target)
        updateProgress()
    }

    private fun updateProgress() {
        val player = engine ?: return
        if (isUserSeeking) return
        val position = player.currentPosition
        if (binding.sbProgress.max <= 0 && player.duration > 0) {
            binding.sbProgress.max = player.duration.toInt()
            binding.tvDuration.text = formatTime(player.duration)
        }
        binding.sbProgress.progress = position.toInt()
        binding.sbProgress.secondaryProgress = player.bufferedPosition.toInt()
        binding.tvCurrent.text = formatTime(position)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.ivPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_audio_pause else R.drawable.ic_audio_play
        )
    }

    private fun updateSubtitle(isPlaying: Boolean) {
        binding.tvSubtitle.setText(
            if (isPlaying) R.string.audio_state_playing else R.string.audio_state_paused
        )
    }

    private fun startProgressTicker() {
        mainHandler.removeCallbacks(progressTicker)
        mainHandler.post(progressTicker)
    }

    private fun stopProgressTicker() {
        mainHandler.removeCallbacks(progressTicker)
    }

    /** 时长格式化为 mm:ss 或 h:mm:ss */
    private fun formatTime(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0L)) / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * 取播放地址：优先取外部 App 通过 [Intent.ACTION_VIEW] 传入的 data，
     * 其次取内部调用通过 [KEY_AUDIO_URL_STRING] 传入的字符串地址。
     */
    private fun resolveMediaUri(): String? {
        if (Intent.ACTION_VIEW == intent.action) {
            intent.data?.let { return it.toString() }
        }
        return intent.getStringExtra(KEY_AUDIO_URL_STRING)
    }

    private fun resolveTitle(): String {
        return intent.getStringExtra(KEY_AUDIO_TITLE_STRING)
            ?: getString(R.string.audio_default_title)
    }

    /** 解析调用方指定的引擎类型，非法或未指定时回退到全局默认（ExoPlayer） */
    private fun resolveEngineType(): AudioEngineType {
        val name = intent.getStringExtra(KEY_AUDIO_ENGINE_NAME) ?: return AudioPlayerEngines.defaultType
        return runCatching { AudioEngineType.valueOf(name) }
            .getOrDefault(AudioPlayerEngines.defaultType)
    }

    override fun onPause() {
        super.onPause()
        engine?.pause()
        stopProgressTicker()
    }

    override fun onResume() {
        super.onResume()
        startProgressTicker()
    }

    override fun onDestroy() {
        // 释放播放器需在 super.onDestroy() 之前，否则基类会先把 binding 置空
        stopProgressTicker()
        engine?.release()
        engine = null
        super.onDestroy()
    }
}
