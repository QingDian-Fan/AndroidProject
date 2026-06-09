package com.common.image.bitmap

import android.graphics.Bitmap

data class CompressOptions @JvmOverloads constructor(
    val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    val quality: Int = 85,
    val minQuality: Int = 40,
    val maxWidth: Int = 0,
    val maxHeight: Int = 0,
    val maxBytes: Int = 0
)
