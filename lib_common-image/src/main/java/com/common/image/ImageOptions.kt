package com.common.image

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.Px

data class ImageOptions @JvmOverloads constructor(
    @field:DrawableRes val placeholderRes: Int = 0,
    @field:DrawableRes val errorRes: Int = 0,
    val placeholderDrawable: Drawable? = null,
    val errorDrawable: Drawable? = null,
    val scaleType: ImageScaleType = ImageScaleType.CENTER_CROP,
    @field:Px val radiusPx: Int = 0,
    val circleCrop: Boolean = false,
    val thumbnail: Float = 0f,
    val skipMemoryCache: Boolean = false,
    val diskCacheStrategy: ImageDiskCacheStrategy = ImageDiskCacheStrategy.AUTOMATIC,
    val crossFade: Boolean = true,
    @field:Px val overrideWidth: Int = 0,
    @field:Px val overrideHeight: Int = 0
)
