package com.dian.demo.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri

object BitmapUtils {
    // 通过uri加载图片
    fun getBitmapFromUri(context: Context, uri: Uri?): Bitmap? {
        try {
            val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri!!, "r") ?: return null
            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}