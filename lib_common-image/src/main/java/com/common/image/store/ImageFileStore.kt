package com.common.image.store

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.common.image.bitmap.BitmapCompressor
import com.common.image.bitmap.CompressOptions
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageFileStore {
    @JvmStatic
    @JvmOverloads
    fun getImageCacheDir(context: Context, childDir: String = DEFAULT_CACHE_DIR): File {
        return File(context.cacheDir, childDir).apply { mkdirs() }
    }

    @JvmStatic
    @JvmOverloads
    fun createCacheFile(
        context: Context,
        extension: String = DEFAULT_EXTENSION,
        prefix: String = DEFAULT_PREFIX,
        childDir: String = DEFAULT_CACHE_DIR
    ): File {
        val normalizedExtension = extension.trim().trimStart('.').ifBlank { DEFAULT_EXTENSION }
        val fileName = "$prefix${System.currentTimeMillis()}_${UUID.randomUUID()}.$normalizedExtension"
        return File(getImageCacheDir(context, childDir), fileName)
    }

    @JvmStatic
    @JvmOverloads
    fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        options: CompressOptions = CompressOptions(),
        fileName: String? = null,
        childDir: String = DEFAULT_CACHE_DIR
    ): File {
        val extension = options.format.toExtension()
        val file = if (fileName.isNullOrBlank()) {
            createCacheFile(context, extension, childDir = childDir)
        } else {
            File(getImageCacheDir(context, childDir), fileName)
        }
        return BitmapCompressor.compressToFile(bitmap, file, options)
    }

    @JvmStatic
    @JvmOverloads
    fun copyUriToCache(
        context: Context,
        uri: Uri,
        fileName: String? = null,
        extension: String = DEFAULT_EXTENSION,
        childDir: String = DEFAULT_CACHE_DIR
    ): File? {
        val file = if (fileName.isNullOrBlank()) {
            createCacheFile(context, extension, childDir = childDir)
        } else {
            File(getImageCacheDir(context, childDir), fileName)
        }

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return null
            file
        }.getOrNull()
    }

    private fun Bitmap.CompressFormat.toExtension(): String {
        return when (this) {
            Bitmap.CompressFormat.PNG -> "png"
            Bitmap.CompressFormat.WEBP -> "webp"
            else -> DEFAULT_EXTENSION
        }
    }

    private const val DEFAULT_CACHE_DIR = "images"
    private const val DEFAULT_EXTENSION = "jpg"
    private const val DEFAULT_PREFIX = "IMG_"
}
