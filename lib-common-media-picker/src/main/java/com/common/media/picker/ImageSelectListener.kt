package com.common.media.picker

interface ImageSelectListener {
    fun selectListener(selectList: ArrayList<String>)
}

interface ImageCancelListener {
    fun cancel()
}