package com.common.utils

import android.content.Context
import android.os.Environment.MEDIA_MOUNTED
import android.os.Environment.getExternalStorageState
import java.io.File
import java.math.BigDecimal


object CacheUtil {

    fun getTotalCacheSize(context: Context): String {
        var cacheSize = getFolderSize(context.cacheDir)
        if (getExternalStorageState() == MEDIA_MOUNTED) {
            cacheSize += getFolderSize(context.externalCacheDir)
            cacheSize += getFolderSize(context.cacheDir)
        }
        LogUtil.e("TAG-Cache", "Cache-Size:${cacheSize.toDouble()}")
        return getFormatSize(cacheSize.toDouble())
    }

    fun clearAllCache(context: Context) {
        deleteDir(context.cacheDir)
        if (getExternalStorageState() == MEDIA_MOUNTED) {
            deleteDir(context.externalCacheDir)
            deleteDir(context.cacheDir)
        }
    }

    fun deleteDir(dir: File?): Boolean {
        val targetDir = dir ?: return false
        if (!targetDir.exists()) {
            return true
        }
        if (targetDir.isDirectory) {
            val children = targetDir.list() ?: return false
            for (i in children.indices) {
                val success = deleteDir(File(targetDir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        return targetDir.delete()
    }

    // 获取文件
    // Context.getExternalFilesDir() --> SDCard/Android/data/你的应用的包名/files/
    // 目录，一般放一些长时间保存的数据
    // Context.getExternalCacheDir() -->
    // SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
    // 获取文件
    // Context.getExternalFilesDir() --> SDCard/Android/data/你的应用的包名/files/
    // 目录，一般放一些长时间保存的数据
    // Context.getExternalCacheDir() -->
    // SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
    @Throws(Exception::class)
    private fun getFolderSize(file: File?): Long {
        var size: Long = 0
        try {
            file?.let {
                val fileList = it.listFiles() ?: return 0
                for (i in fileList.indices) {
                    // 如果下面还有文件
                    size = if (fileList[i].isDirectory) {
                        size + getFolderSize(fileList[i])
                    } else {
                        size + fileList[i].length()
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * 格式化单位
     *
     * @param size
     */
    private fun getFormatSize(size: Double): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) {
            return "$size Byte"
        }
        val megaByte = kiloByte / 1024
        if (megaByte < 1) {
            val result1 = BigDecimal(kiloByte.toString())
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " KB"
        }
        val gigaByte = megaByte / 1024
        if (gigaByte < 1) {
            val result2 = BigDecimal(megaByte.toString())
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " MB"
        }
        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) {
            val result3 = BigDecimal(gigaByte.toString())
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " GB"
        }
        val result4 = BigDecimal(teraBytes)
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + " TB"
    }
}
