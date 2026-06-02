package com.common.weight.webview.callback

/** [com.common.weight.webview.webview.BrowserWebView.getShareData] 抽取页面分享信息后回调。 */
fun interface IShareCallBack {
    fun onShareData(
        url: String,
        covers: List<String>,
        title: String,
        desc: String
    )
}
