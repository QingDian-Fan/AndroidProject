package com.dian.demo.utils.webview.callback

interface IShareCallBack {
    fun onShareData(
        url: String,
        covers: MutableList<String?>,
        title: String,
        desc: String
    )
}