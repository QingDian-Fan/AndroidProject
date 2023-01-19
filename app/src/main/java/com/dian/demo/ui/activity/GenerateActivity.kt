package com.dian.demo.ui.activity

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.widget.Toast
import androidx.core.view.drawToBitmap
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityGenerateBinding
import com.dian.demo.utils.ResourcesUtils
import com.dian.demo.utils.code.generate.GenerateCodeUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GenerateActivity : BaseAppBindActivity<ActivityGenerateBinding>() {

    companion object {

        @JvmStatic
        fun start(mActivity: Activity) {
            val intent = Intent()
            intent.setClass(mActivity, GenerateActivity::class.java)
            mActivity.startActivity(intent)
        }
    }

    private var codeBitmap: Bitmap? = null

    override fun getLayoutId(): Int = R.layout.activity_generate

    /**
     *  初始化操作
     */
    override fun initialize(savedInstanceState: Bundle?) {
        setPageTitle(ResourcesUtils.getString(R.string.generate_qr_code))
        binding.tvGenerate.setOnClickListener {
            if (binding.etContent.text == null || TextUtils.isEmpty(binding.etContent.text.toString())) return@setOnClickListener
            val contentString = binding.etContent.text.toString()
            val portrait = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_pink)
            codeBitmap = GenerateCodeUtils.createQRCodeBitmap(contentString, portrait, 1024, 170)
            binding.ivCode.setImageBitmap(codeBitmap)
        }

        /* mIvCode.isDrawingCacheEnabled = true //禁用绘图缓存
            mIvCode.buildDrawingCache()
            val temBitmap: Bitmap = mIvCode.drawingCache
            val result: String = QrCodeUtils.parseQRcode(temBitmap)
            Toast.makeText(this, "长按识别二维码结果:$result", Toast.LENGTH_LONG).show()
            //禁用DrawingCahce否则会影响性能 ,而且不禁止会导致每次截图到保存的是缓存的位图
            mIvCode.isDrawingCacheEnabled = false //识别完成之后开启绘图缓存*/
        binding.ivCode.setOnLongClickListener {
            saveBitmapGallery(
                this@GenerateActivity,
                binding.ivCode.drawToBitmap(),
                "QR_Code_" + System.currentTimeMillis()
            )
            false
        }
    }

    private fun saveBitmapGallery(mContext: Context, bmp: Bitmap, picName: String) {
        val fileName: String?
        //系统相册目录
        val galleryPath = (Environment.getExternalStorageDirectory()
            .toString() + File.separator + Environment.DIRECTORY_DCIM
                + File.separator + "Camera" + File.separator)
        // 声明文件对象
        var file: File? = null
        // 声明输出流
        var outStream: FileOutputStream? = null
        try {
            // 如果有目标文件，直接获得文件对象，否则创建一个以filename为名称的文件
            file = File(galleryPath, "$picName.jpg")
            // 获得文件相对路径
            fileName = file.toString()
            // 获得输出流，如果文件中有内容，追加内容
            outStream = FileOutputStream(fileName)
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, outStream)
            Toast.makeText(
                mContext,
                ResourcesUtils.getString(R.string.save_success),
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            e.stackTrace
        } finally {
            try {
                outStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DATA, file!!.absolutePath)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // 最后通知图库更新
        mContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
    }
}