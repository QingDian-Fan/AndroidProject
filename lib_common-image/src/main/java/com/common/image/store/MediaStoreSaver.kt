package com.common.image.store

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.common.image.bitmap.BitmapCompressor
import com.common.image.bitmap.CompressOptions
import java.io.File
import java.io.FileOutputStream

object MediaStoreSaver {
    @JvmStatic
    @JvmOverloads
    fun saveBitmap(
        context: Context,
        bitmap: Bitmap,
        displayName: String = defaultDisplayName(),
        relativePath: String = Environment.DIRECTORY_PICTURES,
        options: CompressOptions = CompressOptions(quality = 90)
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveBitmapQ(context, bitmap, displayName, relativePath, options)
        } else {
            saveBitmapLegacy(context, bitmap, displayName, relativePath, options)
        }
    }

    private fun saveBitmapQ(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        relativePath: String,
        options: CompressOptions
    ): Uri? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName.ensureExtension(options.format))
            put(MediaStore.Images.Media.MIME_TYPE, options.format.toMimeType())
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return runCatching {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(BitmapCompressor.compressToBytes(bitmap, options))
            } ?: return null

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        }.getOrElse {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun saveBitmapLegacy(
        context: Context,
        bitmap: Bitmap,
        displayName: String,
        relativePath: String,
        options: CompressOptions
    ): Uri? {
        val directory = Environment.getExternalStoragePublicDirectory(relativePath).apply { mkdirs() }
        val file = File(directory, displayName.ensureExtension(options.format))
        return runCatching {
            FileOutputStream(file).use { output ->
                output.write(BitmapCompressor.compressToBytes(bitmap, options))
            }
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                arrayOf(options.format.toMimeType()),
                null
            )
            Uri.fromFile(file)
        }.getOrNull()
    }

    private fun defaultDisplayName(): String {
        return "IMG_${System.currentTimeMillis()}.jpg"
    }

    private fun String.ensureExtension(format: Bitmap.CompressFormat): String {
        val extension = format.toExtension()
        return if (contains(".")) this else "$this.$extension"
    }

    private fun Bitmap.CompressFormat.toExtension(): String {
        return when (this) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> "jpg"
        }
    }

    private fun Bitmap.CompressFormat.toMimeType(): String {
        return when (this) {
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.WEBP -> "image/webp"
            else -> "image/jpeg"
        }
    }
}
