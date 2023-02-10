package com.dian.demo.ui.dialog

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.invisible
import com.demo.project.utils.ext.visible
import com.dian.demo.databinding.DialogUpdateBinding
import com.dian.demo.http.HttpUtils
import com.dian.demo.http.SingleDownloader
import com.dian.demo.utils.IntentUtil
import com.dian.demo.utils.ext.singleClick
import java.io.File


class UpdateDialog : AppCompatDialogFragment() {

    companion object {
        fun getDialog(): AppCompatDialogFragment {
            return UpdateDialog()
        }
    }

    private lateinit var binding: DialogUpdateBinding
    private val downloadUrl =
        "https://cdn.mytoken.org/app_download/MT-mytoken-hk-release-3.3.4_mytoken_aligned_signed.apk"
    private val apkPath =
        Environment.getExternalStorageDirectory().path + "/DomeProject/apk/"
    private val apkName = "玩Android.apk"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(context), container, false)

        initData()
        return binding.root

    }

    private fun initData() {
        binding.tvCancel.singleClick {
            dismissAllowingStateLoss()
        }
        binding.pbUpdateProgress.max = 100
        binding.tvConfirm.singleClick {
            if (binding.pbUpdateProgress.visibility == invisible) {
                binding.pbUpdateProgress.visibility = visible
            }
            if (binding.pbUpdateProgress.visibility == visible) {
                binding.tvCancel.visibility = gone
                binding.tvConfirm.setOnClickListener(null)
            }
            binding.pbUpdateProgress.progress = 0
            binding.tvConfirm.text = "下载中 0%"
            createDir()
            SingleDownloader(HttpUtils.getInstance().getClient())
                .onProgress { currentLength, totalLength, progress ->
                    Log.e("SingleDownloader", "--->onProgress:$progress")
                    binding.pbUpdateProgress.progress = progress
                    binding.tvConfirm.text = "下载中 $progress%"

                }
                .onCompletion { url, filePath ->
                    Log.e("SingleDownloader", "--->onCompletion")
                    IntentUtil.installedApp(requireContext(), filePath)
                }
                .onError { url, cause ->
                    Log.e("SingleDownloader", "--->onError${cause.message}")
                }
                .onSuccess { url, file ->
                    Log.e("SingleDownloader", "--->onSuccess")
                }
                .excute(downloadUrl, "$apkPath$apkName")
        }
    }

    private fun createDir() {
        val dir = File(apkPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog!!.setCanceledOnTouchOutside(false)
            if (dialog!!.window != null) {
                dialog!!.window!!.setBackgroundDrawableResource(android.R.color.transparent)
                val params = dialog!!.window?.attributes
                params?.width = ViewGroup.LayoutParams.MATCH_PARENT
                params?.height = ViewGroup.LayoutParams.MATCH_PARENT
                params?.gravity = Gravity.CENTER
                dialog!!.window?.attributes = params
            }
        }
    }
}