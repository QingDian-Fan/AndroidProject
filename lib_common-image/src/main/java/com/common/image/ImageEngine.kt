package com.common.image

import android.content.Context
import android.widget.ImageView

interface ImageEngine {
    fun load(request: ImageRequest)

    fun clear(imageView: ImageView)

    fun pause(context: Context)

    fun resume(context: Context)

    fun clearMemory(context: Context)

    suspend fun clearDiskCache(context: Context)
}
