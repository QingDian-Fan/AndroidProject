package com.dian.demo.utils.webview.callback

import android.webkit.WebResourceRequest
import android.webkit.WebView


interface WebViewCallBack {

    fun pageStarted(url: String?)

    fun pageFinished(url: String?)

    fun pageError()

    fun updateTitle(title: String?)

    fun overrideUrlLoading(view: WebView?, url: WebResourceRequest?): Boolean
}