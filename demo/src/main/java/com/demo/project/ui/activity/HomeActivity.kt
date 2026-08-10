package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import com.common.media.picker.ImageCancelListener
import com.common.media.picker.ImageSelectListener
import com.common.media.picker.ImageSelectUtil
import com.common.media.picker.MediaType
import com.common.scan.WeChatQRCodeActivity
import com.common.scan.camera.CameraScan
import com.common.ui.BaseAppVMActivity
import com.common.utils.ToastUtil
import com.demo.project.constants.ANDROID_ASSET_URI
import com.demo.project.R
import com.demo.project.databinding.ActivityMainBinding
import com.demo.project.player.audio.AudioEngineType
import com.demo.project.vm.MainViewModel

class HomeActivity : BaseAppVMActivity<ActivityMainBinding, MainViewModel>() {
    companion object {
        private const val EXIT_INTERVAL_MS = 2_000L

        /** 演示用的公开测试视频地址，不含任何鉴权参数，仅用于本演示入口 */
        private const val DEMO_VIDEO_URL =
            "https://oss.qinxuestudy.com/fangtian-education/homework/2026/06/18/" +
                    "2052631338333810690_1781780255897/VID_20260520_153925.mp4"

        @JvmStatic
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, HomeActivity::class.java).apply {
                if (mContext !is Activity) {
                    flags=FLAG_ACTIVITY_NEW_TASK
                }
            }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_main
    val mSelectList = ArrayList<String>()
    private var lastBackPressedAt: Long? = null

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.setCenterText(getString(R.string.home_page_title))
        // 首页是任务栈根页面，没有可返回的上级页面，隐藏标题栏左上角返回按钮
        hideActionBack()
        binding.btnScanActivity.setOnClickListener {
            startActivityForResult(WeChatQRCodeActivity::class.java)
        }
        binding.btnCameraActivity.setOnClickListener {
            CameraActivity.start(this@HomeActivity)
        }
        binding.btnWebActivity.setOnClickListener {
            WebActivity.start(this@HomeActivity)
        }
        binding.btnBrowseActivity.setOnClickListener {
            WebExplorerActivity.start(this@HomeActivity,"${ANDROID_ASSET_URI}demo.html")
        }
        binding.btnDebugActivity.setOnClickListener {
            DebugActivity.start(this@HomeActivity)
        }
        binding.btnVideoActivity.setOnClickListener {
            // 演示入口显式传入测试地址；正式入口不再对空地址做静默兜底
            VideoPlayerActivity.start(this@HomeActivity, DEMO_VIDEO_URL)
        }
        binding.btnAudioActivity.setOnClickListener {
            AudioPlayerActivity.start(this@HomeActivity, engineType =  AudioEngineType.FFMPEG)
        }
        binding.btnThemeSettingsActivity.setOnClickListener {
            ThemeSettingsActivity.start(this@HomeActivity)
        }
        binding.btnSelectActivity.setOnClickListener {
            ImageSelectUtil()
                .setActivity(this@HomeActivity)
                .setMaxSelect(5)
                .setMediaType(MediaType.ALL)
                .setSelectList(mSelectList)
                .setColumn(3)
                .setSelectListener(object : ImageSelectListener {
                    override fun selectListener(selectList: ArrayList<String>) {
                        mSelectList.clear()
                        ToastUtil.showToast(
                            this@HomeActivity,
                            "size::${selectList.size}"
                        )
                        if (selectList.isNotEmpty()) {
                            selectList.forEach {
                                mSelectList.add(it)
                            }
                        }
                    }
                })
                .setCancelListener(object : ImageCancelListener {
                    override fun cancel() {
                        mSelectList.clear()
                        ToastUtil.showToast(this@HomeActivity, getString(R.string.toast_selection_cancelled))
                    }
                })
                .create()
        }

    }

    override fun handleBackPress(): Boolean {
        val now = SystemClock.elapsedRealtime()
        val previousBackPressedAt = lastBackPressedAt
        if (previousBackPressedAt != null && now - previousBackPressedAt <= EXIT_INTERVAL_MS) {
            finish()
            return true
        }
        lastBackPressedAt = now
        showToast(R.string.toast_press_again_to_exit)
        return true
    }

    private val startActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            processQRCodeResult(result.data)
        }
    }
    private fun processQRCodeResult(intent: Intent?) {
        // 扫码结果
        CameraScan.parseScanResult(intent)?.let {

            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
        }
    }
    private fun startActivityForResult(clazz: Class<*>) {
        val options = ActivityOptionsCompat.makeCustomAnimation(
            this, R.anim.alpha_in, R.anim.alpha_out
        )
        startActivityLauncher.launch(Intent(this, clazz), options)
    }

    override fun getViewModelClass(): Class<MainViewModel> = MainViewModel::class.java
}
