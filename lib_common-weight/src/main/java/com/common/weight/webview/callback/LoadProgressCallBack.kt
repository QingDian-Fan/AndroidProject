package com.common.weight.webview.callback

/** WebView 加载进度回调（0..100）。 */
fun interface LoadProgressCallBack {
    fun onCurrentProgress(currentProgress: Int)
}
