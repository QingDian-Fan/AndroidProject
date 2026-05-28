package com.project.common.view.webview.callback

interface IShareCallBack {
    fun onShareData(
        url: String,
        covers: MutableList<String?>,
        title: String,
        desc: String
    )
}