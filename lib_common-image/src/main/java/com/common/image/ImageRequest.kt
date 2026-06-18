package com.common.image

import android.content.Context
import android.widget.ImageView

data class ImageRequest(
    val context: Context,
    val target: ImageView,
    val source: Any?,
    val options: ImageOptions = ImageOptions()
)
