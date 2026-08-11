package com.demo.project.ui.dialog

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.common.image.ext.loadImage
import com.common.share.ShareUtils
import com.common.share.temp.ShareTempFileStore
import com.common.share.temp.ShareTempFiles
import com.common.utils.LogUtil
import com.common.utils.ResourcesUtil
import com.common.utils.ScreenShotUtil
import com.common.utils.ToastUtil.showToast
import com.common.utils.code.generate.GenerateCodeUtils
import com.common.utils.ext.gone
import com.common.utils.ext.visible
import com.common.utils.moshi.MoshiUtil
import com.demo.project.ProjectApplication.Companion.getAppContext
import com.demo.project.R
import com.demo.project.databinding.DialogWebShareBinding
import com.demo.project.ui.adapter.WebShareLogoAdapter


class WebShareDialog : AppCompatDialogFragment() {
    companion object {
        /** 系统分享面板不属于 Channel 中的任何具体三方渠道，仅作为会话的渠道标记 */
        private const val CHANNEL_SYSTEM_CHOOSER = 0

        fun getDialog(
            url: String,
            covers: MutableList<String?>,
            title: String,
            desc: String
        ): WebShareDialog {
            val dialog = WebShareDialog()
            val bundle = Bundle()
            bundle.putString("url", url)
            bundle.putString("title", title)
            bundle.putString("desc", desc)
            bundle.putString("covers", MoshiUtil.toJson(covers))
            dialog.arguments = bundle
            return dialog
        }
    }

    private lateinit var binding: DialogWebShareBinding
    private var url: String? = ""
    private var title: String? = ""
    private var contentString: String? = ""

    /** 防止快速连续点击重复生成临时图片；弹窗关闭即销毁，无需在 onDestroyView 复位 */
    private var sharing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreate(savedInstanceState)
        binding = DialogWebShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initData()
    }

    private fun initView() {
        url = arguments?.getString("url")
        title = arguments?.getString("title")
        contentString = arguments?.getString("desc")
        val coversString = arguments?.getString("covers")
        binding.rlRoot.setOnClickListener {
            dismissAllowingStateLoss()
        }
        binding.cardContainer.setOnClickListener {  }
        coversString?.let {
            val covers = MoshiUtil.fromJson<MutableList<String?>>(it)
            val allCovers = arrayListOf<String?>().apply {
                add(null)
                covers?.filter { !it.isNullOrBlank() }
                    ?.forEach { add(it) }
            }
            if (allCovers.size>=2){
                binding.mainImage.visible()
                binding.mainImage.loadImage(allCovers.getOrNull(1)?.toUri())

            }else{
                binding.mainImage.gone()
            }
            context?.let { context ->
                binding.rvData.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
                binding.rvData.adapter = WebShareLogoAdapter(allCovers) { position ->
                    if (position == 0) {
                        binding.mainImage.gone()
                    } else {
                        binding.mainImage.visible()
                        binding.mainImage.loadImage(allCovers.getOrNull(position)?.toUri())
                    }
                }
            }
        }

        url?.let {
            val codeBitmap = GenerateCodeUtils.createQRCodeBitmap(url, null, 1024, 150)
            binding.ivCode.setImageBitmap(codeBitmap)
        }

        title?.let {
            binding.title.text = it
        }
        contentString?.let {
            binding.tvContent.text = it
        }
        binding.btnShare.setOnClickListener {
            if (sharing) return@setOnClickListener
            sharing = true
            val shareBitmap = ScreenShotUtil.getViewBitmap(binding.cardContainer)
            if (shareBitmap == null) {
                sharing = false
                showToast(
                    getAppContext(),
                    ResourcesUtil.getString(com.common.share.R.string.share_image_failed),
                    false,
                    Gravity.CENTER
                )
                return@setOnClickListener
            }
            shareBitmap(shareBitmap)
            dismissAllowingStateLoss()
        }
        binding.btnSave.setOnClickListener {
            val shareBitmap = ScreenShotUtil.getViewBitmap(binding.cardContainer)
            ShareUtils.saveBmp2Uri(context, shareBitmap, "share" + System.currentTimeMillis())
            showToast(getAppContext(), ResourcesUtil.getString(R.string.img_is_save), false, Gravity.CENTER)
            dismissAllowingStateLoss()
        }

    }

    private fun initData() {


    }


    override fun onStart() {
        super.onStart()
        dialog?.let {
            it.setCanceledOnTouchOutside(true)
            it.window?.run {
                // 与弹窗底部区域同色，随日夜间模式切换，避免夜间模式下底部导航栏仍为白色
                // bg_common 定义在 lib_common-theme，非传递 R 类下需全限定引用
                navigationBarColor =
                    ContextCompat.getColor(context, com.common.theme.R.color.bg_common)

                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                setBackgroundDrawableResource(R.color.transparent)
                val params = attributes
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
                params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                attributes = params
            }
        }
    }

    /**
     * 把分享卡片写入本次分享会话的临时目录，并调起系统分享面板。
     *
     * 系统面板无法证明接收方已经读完文件，因此这里不在 [startActivity] 之后立即删除，
     * 而是进入「安全读取期 + 过期扫描」组合清理；调起失败或生成失败则立即清理半成品。
     */
    private fun shareBitmap(bitmap: Bitmap) {
        val activity = activity ?: return
        val format = Bitmap.CompressFormat.PNG
        // 系统面板走 FileProvider content URI，使用内部缓存即可
        val session = ShareTempFiles.beginSession(activity, CHANNEL_SYSTEM_CHOOSER, false)
        if (session == null) {
            notifyShareFailed()
            return
        }

        val file = ShareTempFiles.writeBitmap(session, bitmap, format, 100)
        if (file == null) {
            ShareTempFiles.finishNow(session.sessionId)
            notifyShareFailed()
            return
        }

        val contentUri = ShareTempFiles.shareUri(activity, session, file)
        if (contentUri == null) {
            // 无法生成可分享 URI，清理半成品并提示失败，不继续传递空路径
            ShareTempFiles.finishNow(session.sessionId)
            notifyShareFailed()
            return
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = ShareTempFiles.mimeTypeOf(format)
            putExtra(Intent.EXTRA_STREAM, contentUri)
            // 部分接收方只认 ClipData 携带的授权，两者同时设置
            clipData = ClipData.newRawUri(file.name, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, getString(R.string.web_share_chooser_title))
        try {
            activity.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            // 没有可用目标应用，文件不会被读取，立即清理
            LogUtil.printStackTrace(e)
            ShareTempFiles.finishNow(session.sessionId)
            notifyShareFailed()
            return
        }
        // 面板已调起：预留集中配置的安全读取时间后删除，超时未删也会被过期扫描兜底
        ShareTempFiles.finishAfterSafeDelay(
            session.sessionId,
            ShareTempFileStore.CHOOSER_SAFE_DELETE_DELAY_MILLIS
        )
    }

    private fun notifyShareFailed() {
        showToast(
            getAppContext(),
            ResourcesUtil.getString(com.common.share.R.string.share_image_failed),
            false,
            Gravity.CENTER
        )
    }
}