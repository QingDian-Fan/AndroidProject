package com.common.image.ext

import android.widget.ImageView
import com.common.image.CommonImage
import com.common.image.ImageRequestBuilder

fun ImageView.loadImage(source: Any?, block: ImageRequestBuilder.() -> Unit = {}) {
    CommonImage.builder(this, source)
        .apply(block)
        .load()
}

fun ImageView.clearImage() {
    CommonImage.clear(this)
}
