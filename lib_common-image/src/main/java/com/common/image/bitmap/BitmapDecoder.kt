package com.common.image.bitmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.File

object BitmapDecoder {
    @JvmStatic
    @JvmOverloads
    fun decodeFile(
        file: File,
        reqWidth: Int = 0,
        reqHeight: Int = 0,
        fixOrientation: Boolean = true
    ): Bitmap? {
        if (!file.exists()) return null

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        return if (fixOrientation) {
            ExifUtils.rotateIfRequired(bitmap, ExifUtils.readOrientation(file.absolutePath))
        } else {
            bitmap
        }
    }

    @JvmStatic
    @JvmOverloads
    fun decodeUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 0,
        reqHeight: Int = 0,
        fixOrientation: Boolean = true
    ): Bitmap? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(ByteArrayInputStream(bytes), null, boundsOptions)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, reqWidth, reqHeight)
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
        return if (fixOrientation) {
            ExifUtils.rotateIfRequired(bitmap, ExifUtils.readOrientation(context, uri))
        } else {
            bitmap
        }
    }

    @JvmStatic
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth <= 0 && reqHeight <= 0) return 1

        val height = options.outHeight
        val width = options.outWidth
        if (height <= 0 || width <= 0) return 1

        var inSampleSize = 1
        while (
            (reqHeight > 0 && height / inSampleSize > reqHeight) ||
            (reqWidth > 0 && width / inSampleSize > reqWidth)
        ) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
