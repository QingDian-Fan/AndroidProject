package com.common.image

import android.content.Context
import android.widget.ImageView
import com.common.image.engine.GlideImageEngine

object CommonImage {
    @Volatile
    private var engine: ImageEngine = GlideImageEngine()

    @JvmStatic
    @JvmOverloads
    fun init(engine: ImageEngine = GlideImageEngine()) {
        this.engine = engine
    }

    @JvmStatic
    @JvmOverloads
    fun load(imageView: ImageView, source: Any?, options: ImageOptions = ImageOptions()) {
        engine.load(ImageRequest(imageView.context, imageView, source, options))
    }

    @JvmStatic
    fun builder(imageView: ImageView, source: Any?): ImageRequestBuilder {
        return ImageRequestBuilder(imageView, source)
    }

    @JvmStatic
    fun clear(imageView: ImageView) {
        engine.clear(imageView)
    }

    @JvmStatic
    fun pause(context: Context) {
        engine.pause(context)
    }

    @JvmStatic
    fun resume(context: Context) {
        engine.resume(context)
    }

    @JvmStatic
    fun clearMemory(context: Context) {
        engine.clearMemory(context)
    }

    @JvmStatic
    suspend fun clearDiskCache(context: Context) {
        engine.clearDiskCache(context)
    }
}
