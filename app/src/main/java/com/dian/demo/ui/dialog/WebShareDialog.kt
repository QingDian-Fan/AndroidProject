package com.dian.demo.ui.dialog

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.bumptech.glide.Glide
import com.dian.demo.BuildConfig
import com.dian.demo.ProjectApplication.Companion.getAppContext
import com.dian.demo.R
import com.dian.demo.databinding.DialogWebShareBinding
import com.dian.demo.ui.adapter.WebShareLogoAdapter
import com.dian.demo.utils.MoshiUtil
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.ScreenShotUtil
import com.dian.demo.utils.ToastUtil.showToast
import com.dian.demo.utils.code.generate.GenerateCodeUtils
import com.dian.demo.utils.ext.gone
import com.dian.demo.utils.ext.visible
import com.dian.demo.utils.share.ShareUtils
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
    ): View? {
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
            Glide.with(this).load(allCovers.getOrNull(0)?.toUri()).into(binding.mainImage)
            context?.let { context ->
                binding.rvData.layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
                binding.rvData.adapter = WebShareLogoAdapter(allCovers) { position ->
                    if (position==0){
                        binding.mainImage.gone()
                    }else{
                        binding.mainImage.visible()
                        Glide.with(this).load(allCovers.getOrNull(position)?.toUri())
                            .into(binding.mainImage)
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
            shareBitmap(shareBitmap,"share-${System.currentTimeMillis()}.png")
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
                navigationBarColor = Color.WHITE

                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

                setBackgroundDrawableResource(android.R.color.transparent)
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
            startActivity(Intent.createChooser(intent, "分享图片"))
        }

    }
}