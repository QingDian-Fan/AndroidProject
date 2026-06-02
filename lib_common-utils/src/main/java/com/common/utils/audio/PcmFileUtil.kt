package com.common.utils.audio

import android.content.Context
import com.common.utils.LogUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PcmFileUtil {

    private const val TAG = "PcmFileUtil"

    private var fout: FileOutputStream? = null

    fun getConvertResultFile(context: Context, fileName: String): String {
        return context.filesDir.absolutePath + fileName
    }

    fun init(context: Context, fileName: String) {
        try {
            fout?.close()
            fout = null
            val path = context.filesDir.absolutePath + fileName
            val file = File(path)
            if (file.exists() && !file.delete()) {
                LogUtil.e(TAG, "删除旧文件失败: $path")
                return
            }
            val parent = file.parentFile
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                LogUtil.e(TAG, "创建父目录失败: ${parent.absolutePath}")
                return
            }
            if (!file.createNewFile()) {
                LogUtil.e(TAG, "创建文件失败: $path")
                return
            }
            fout = FileOutputStream(file)
        } catch (e: IOException) {
            LogUtil.e(TAG, "初始化 PCM 文件失败: ${e.message}")
        }
    }

    fun write(data: ByteArray) {
        try {
            fout?.write(data)
        } catch (e: IOException) {
            LogUtil.e(TAG, "写入 PCM 数据失败: ${e.message}")
        }
    }

    fun close() {
        try {
            fout?.close()
        } catch (e: IOException) {
            LogUtil.e(TAG, "关闭 PCM 文件失败: ${e.message}")
        }
    }
}