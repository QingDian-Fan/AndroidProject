package com.dian.demo.ui.dialog

import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.invisible
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.config.Constant.apkPath
import com.dian.demo.databinding.DialogUpdateBinding
import com.dian.demo.http.HttpUtils
import com.dian.demo.http.SingleDownloader
import com.dian.demo.utils.ExceptionHandlerUtil
import com.dian.demo.utils.IntentUtil
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.ext.singleClick
import java.io.File


class UpdateDialog : AppCompatDialogFragment() {

    companion object {
        fun getDialog(downloadUrl: String, apkName: String): AppCompatDialogFragment {
            val dialog = UpdateDialog()
            val bundle = Bundle()
            bundle.putString("url", downloadUrl)
            bundle.putString("apkName", apkName)
            dialog.arguments = bundle
            return dialog
        }
    }

    private lateinit var binding: DialogUpdateBinding

    private lateinit var downloadUrl: String
    private lateinit var apkName: String


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogUpdateBinding.inflate(LayoutInflater.from(context), container, false)
        arguments?.let {
            downloadUrl = it.getString("url", "https://cdn.mytoken.org/app_download/MT-mytoken-hk-release-3.3.4_mytoken_aligned_signed.apk")
            apkName = it.getString("apkName","玩Android.apk")
        }
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
                    LogUtil.e("SingleDownloader", "--->onProgress:$progress")
                    binding.pbUpdateProgress.progress = progress
                    binding.tvConfirm.text = "下载中 $progress%"

                }
                .onCompletion { url, filePath ->
                    LogUtil.e("SingleDownloader", "--->onCompletion")
                    IntentUtil.installedApp(requireContext(), filePath)
                }
                .onError { url, cause ->
                    LogUtil.e("SingleDownloader", "--->onError${cause.message}")
                }
                .onSuccess { url, file ->
                    LogUtil.e("SingleDownloader", "--->onSuccess")
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