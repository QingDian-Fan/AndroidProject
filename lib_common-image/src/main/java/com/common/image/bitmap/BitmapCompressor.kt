package com.common.image.bitmap

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object BitmapCompressor {
    @JvmStatic
    @JvmOverloads
    fun compressToBytes(bitmap: Bitmap, options: CompressOptions = CompressOptions()): ByteArray {
        val target = bitmap.scaleIfNeeded(options.maxWidth, options.maxHeight)
        var quality = options.quality.coerceIn(1, 100)

        fun encode(currentQuality: Int): ByteArray {
            val output = ByteArrayOutputStream()
            output.use {
                target.compress(options.format, currentQuality, it)
                return it.toByteArray()
            }
        }

        var bytes = encode(quality)
        if (options.maxBytes > 0 && options.format != Bitmap.CompressFormat.PNG) {
            while (bytes.size > options.maxBytes && quality > options.minQuality) {
                quality = (quality - 5).coerceAtLeast(options.minQuality)
                bytes = encode(quality)
            }
        }

        return bytes
    }

    @JvmStatic
    @JvmOverloads
    fun compressToFile(bitmap: Bitmap, file: File, options: CompressOptions = CompressOptions()): File {
        file.parentFile?.mkdirs()
        val bytes = compressToBytes(bitmap, options)
        FileOutputStream(file).use { it.write(bytes) }
        return file
    }

    @JvmStatic
    @JvmOverloads
    fun scale(bitmap: Bitmap, maxWidth: Int = 0, maxHeight: Int = 0): Bitmap {
        return bitmap.scaleIfNeeded(maxWidth, maxHeight)
    }

    private fun Bitmap.scaleIfNeeded(maxWidth: Int, maxHeight: Int): Bitmap {
        if (maxWidth <= 0 && maxHeight <= 0) return this

        val widthRatio = if (maxWidth > 0) maxWidth.toFloat() / width else Float.MAX_VALUE
        val heightRatio = if (maxHeight > 0) maxHeight.toFloat() / height else Float.MAX_VALUE
        val ratio = minOf(widthRatio, heightRatio, 1f)
        if (ratio >= 1f) return this

        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}
