package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
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
import com.demo.project.vm.MainViewModel

class HomeActivity : BaseAppVMActivity<ActivityMainBinding, MainViewModel>() {
    companion object {
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

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.setCenterText("功能菜单")
        binding.btnScanActivity.setOnClickListener {
            startActivityForResult(WeChatQRCodeActivity::class.java)
        }
        binding.btnCameraActivity.setOnClickListener {
            CameraActivity.start(this@HomeActivity)
        }
        binding.btnWebActivity.setOnClickListener {
            WebActivity.start(this@HomeActivity,"${ANDROID_ASSET_URI}demo.html")
        }
        binding.btnBrowseActivity.setOnClickListener {
            WebExplorerActivity.start(this@HomeActivity,"${ANDROID_ASSET_URI}demo.html")
        }
        binding.btnDebugActivity.setOnClickListener {
            DebugActivity.start(this@HomeActivity)
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
                        ToastUtil.showToast(this@HomeActivity, "取消了")
                    }
                })
                .create()
        }

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
   /* @SingleClick
    fun clickView(view: View) {
        when (view.id) {
            R.id.btn_scan_activity->{
                startActivityForResult(WeChatQRCodeActivity::class.java)
            }
            R.id.btn_camera_activity->{
                CameraActivity.start(this@HomeActivity)
            }
            R.id.btn_select_activity->{
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
                            ToastUtil.showToast(this@HomeActivity, "取消了")
                        }
                    })
                    .create()
            }
        }
    }*/

    override fun getViewModelClass(): Class<MainViewModel> = MainViewModel::class.java
}