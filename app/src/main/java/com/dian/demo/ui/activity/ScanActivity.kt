package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Vibrator
import android.text.TextUtils
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityScanBinding
import com.dian.demo.utils.BitmapUtils
import com.dian.demo.utils.PictureSelector
import com.dian.demo.utils.ResourcesUtils
import com.dian.demo.utils.code.core.QRCodeView
import com.dian.demo.utils.code.decoder.QRCodeDecoder
import com.dian.demo.utils.ext.singleClick
import java.util.concurrent.Executors


//top_up_withdrawal
class ScanActivity : BaseAppBindActivity<ActivityScanBinding>(), QRCodeView.Delegate {
    companion object {
        private const val REQ_CODE_SELECT_PIC = 3
        @JvmStatic
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, ScanActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    private var  isSelect:Boolean = false

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_CODE_SELECT_PIC -> {
                PictureSelector.result(resultCode, data)?.let {
                    decodeQrCode(BitmapUtils.getBitmapFromUri(this@ScanActivity, it))
                }
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_scan


    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().visibility = gone
        binding.zxingView.setDelegate(this@ScanActivity)

        binding.ivClose.singleClick {
            onBackPressed()
        }
        binding.ivAlbum.singleClick {
            PictureSelector.select(this@ScanActivity, REQ_CODE_SELECT_PIC)
        }
        binding.ivTorch.singleClick {
            isSelect = !isSelect
            binding.ivTorch.isSelected = isSelect
            if (isSelect) binding.zxingView.openFlashlight()
            else binding.zxingView.closeFlashlight()
        }
    }

    override fun onStart() {
        super.onStart()
        binding.zxingView.startCamera()
        binding.zxingView.startSpotAndShowRect()
        startAnim()
    }

    private fun startAnim() {
        val anim: Animation = AnimationUtils.loadAnimation(this, R.anim.scan_line_anim)
        binding.lineScan.startAnimation(anim)
    }

    override fun onStop() {
        super.onStop()
        binding.zxingView.stopCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.zxingView.onDestroy()
    }

    override fun onScanQRCodeSuccess(result: String?) {
        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)
        if (TextUtils.isEmpty(result)) return
        if (result!!.startsWith("http://", false) || result.startsWith("https://", false)) {
            WebExplorerActivity.start(this@ScanActivity, result)
            finish()
        }
    }


    override fun onCameraAmbientBrightnessChanged(isDark: Boolean) {

    }


    override fun onScanQRCodeOpenCameraError() {

    }

    private fun decodeQrCode(bitmap: Bitmap?) {
        if (bitmap==null){
            showToast(ResourcesUtils.getString(R.string.decode_qr_code_fail))
            return
        }
        Executors.newSingleThreadExecutor().execute {
          val resultString = QRCodeDecoder.syncDecodeQRCode(bitmap)
            runOnUiThread {
                if (TextUtils.isEmpty(resultString)) {
                    showToast(ResourcesUtils.getString(R.string.decode_qr_code_fail))
                }else{
                    if (resultString!!.startsWith("http://", false) || resultString.startsWith("https://", false)) {
                        WebExplorerActivity.start(this@ScanActivity, resultString)
                        finish()
                    }
                }
            }
        }
    }
}