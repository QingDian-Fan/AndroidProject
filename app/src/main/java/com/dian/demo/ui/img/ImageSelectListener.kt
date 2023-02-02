package com.dian.demo.ui.img

interface ImageSelectListener {
    fun selectListener(selectList: ArrayList<String>)
}

interface ImageCancelListener{
    fun cancel()
}