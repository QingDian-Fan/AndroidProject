package com.demo.project.ui.dialog

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.common.image.ext.loadImage
import com.common.share.ShareUtils
import com.common.utils.ResourcesUtil
import com.common.utils.ScreenShotUtil
import com.common.utils.ToastUtil.showToast
import com.common.utils.code.generate.GenerateCodeUtils
import com.common.utils.ext.gone
import com.common.utils.ext.visible
import com.common.utils.moshi.MoshiUtil
import com.demo.project.BuildConfig
import com.demo.project.ProjectApplication.Companion.getAppContext
import com.demo.project.R
import com.demo.project.databinding.DialogWebShareBinding
import com.demo.project.ui.adapter.WebShareLogoAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class WebShareDialog : AppCompatDialogFragment() {
    companion object {
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
            val shareBitmap = ScreenShotUtil.getViewBitmap(binding.cardContainer)
            shareBitmap?.let {
                shareBitmap(it,"share-${System.currentTimeMillis()}.png")
            }
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

    private fun shareBitmap(bitmap: Bitmap, fileName: String) {
       activity?.apply{
            // 保存Bitmap到文件
            val cacheDir: File? = externalCacheDir // 或使用内部缓存 getCacheDir()
            val file = File(cacheDir, fileName)
            try {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // PNG或JPEG
                    out.flush()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }

            // 通过FileProvider获取内容URI
            val contentUri: Uri? = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                file
            )

            // 创建分享Intent
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("image/png") // 根据实际格式调整MIME类型
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // 临时授权

            // 触发系统分享菜单
            startActivity(Intent.createChooser(intent, getString(R.string.web_share_chooser_title)))
        }

    }
}