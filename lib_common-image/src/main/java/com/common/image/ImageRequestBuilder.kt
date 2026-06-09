package com.common.image

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.Px

class ImageRequestBuilder internal constructor(
    private val imageView: ImageView,
    private val source: Any?
) {
    @DrawableRes
    private var placeholderRes: Int = 0

    @DrawableRes
    private var errorRes: Int = 0
    private var placeholderDrawable: Drawable? = null
    private var errorDrawable: Drawable? = null
    private var scaleType: ImageScaleType = ImageScaleType.CENTER_CROP

    @Px
    private var radiusPx: Int = 0
    private var circleCrop: Boolean = false
    private var thumbnail: Float = 0f
    private var skipMemoryCache: Boolean = false
    private var diskCacheStrategy: ImageDiskCacheStrategy = ImageDiskCacheStrategy.AUTOMATIC
    private var crossFade: Boolean = true

    @Px
    private var overrideWidth: Int = 0

    @Px
    private var overrideHeight: Int = 0

    fun placeholder(@DrawableRes resId: Int) = apply {
        placeholderRes = resId
        placeholderDrawable = null
    }

    fun placeholder(drawable: Drawable?) = apply {
        placeholderDrawable = drawable
        placeholderRes = 0
    }

    fun error(@DrawableRes resId: Int) = apply {
        errorRes = resId
        errorDrawable = null
    }

    fun error(drawable: Drawable?) = apply {
        errorDrawable = drawable
        errorRes = 0
    }

    fun centerCrop() = apply {
        scaleType = ImageScaleType.CENTER_CROP
    }

    fun fitCenter() = apply {
        scaleType = ImageScaleType.FIT_CENTER
    }

    fun centerInside() = apply {
        scaleType = ImageScaleType.CENTER_INSIDE
    }

    fun noScale() = apply {
        scaleType = ImageScaleType.NONE
    }

    fun radius(@Px radius: Int) = apply {
        radiusPx = radius.coerceAtLeast(0)
        if (radiusPx > 0) {
            circleCrop = false
        }
    }

    fun circleCrop() = apply {
        circleCrop = true
        radiusPx = 0
    }

    fun thumbnail(sizeMultiplier: Float) = apply {
        thumbnail = sizeMultiplier.coerceIn(0f, 1f)
    }

    fun skipMemoryCache(skip: Boolean = true) = apply {
        skipMemoryCache = skip
    }

    fun diskCache(strategy: ImageDiskCacheStrategy) = apply {
        diskCacheStrategy = strategy
    }

    fun noDiskCache() = apply {
        diskCacheStrategy = ImageDiskCacheStrategy.NONE
    }

    fun crossFade(enable: Boolean = true) = apply {
        crossFade = enable
    }

    fun override(@Px width: Int, @Px height: Int) = apply {
        overrideWidth = width.coerceAtLeast(0)
        overrideHeight = height.coerceAtLeast(0)
    }

    fun into(target: ImageView = imageView) {
        CommonImage.load(target, source, build())
    }

    fun load() {
        into(imageView)
    }

    fun build(): ImageOptions {
        return ImageOptions(
            placeholderRes = placeholderRes,
            errorRes = errorRes,
            placeholderDrawable = placeholderDrawable,
            errorDrawable = errorDrawable,
            scaleType = scaleType,
            radiusPx = radiusPx,
            circleCrop = circleCrop,
            thumbnail = thumbnail,
            skipMemoryCache = skipMemoryCache,
            diskCacheStrategy = diskCacheStrategy,
            crossFade = crossFade,
            overrideWidth = overrideWidth,
            overrideHeight = overrideHeight
        )
    }
}
