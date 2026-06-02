package com.common.weight.webview.callback

import android.webkit.WebResourceRequest
import android.webkit.WebView

/**
 * `WebViewClient` + `WebChromeClient` 关键回调的统一出口，由
 * [com.common.weight.webview.webset.DefaultWebViewClient] 与
 * [com.common.weight.webview.webset.DefaultWebChromeClient] 转发到调用方。
 */
interface WebViewCallBack {

    fun pageStarted(url: String?)

    fun pageFinished(url: String?)

    fun pageError()

    fun updateTitle(title: String?)

    /** 同 `WebViewClient.shouldOverrideUrlLoading` 返回值：true 表示宿主已消费该跳转。 */
    fun overrideUrlLoading(view: WebView?, url: WebResourceRequest?): Boolean
}
