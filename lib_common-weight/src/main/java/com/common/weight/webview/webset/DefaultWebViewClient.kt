package com.common.weight.webview.webset

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.common.weight.webview.callback.WebViewCallBack

/**
 * 默认 `WebViewClient`：所有关键回调透传到 [WebViewCallBack]。
 */
class DefaultWebViewClient(
    private val webViewCallBack: WebViewCallBack
) : WebViewClient() {

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        webViewCallBack.pageStarted(url)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        webViewCallBack.pageFinished(url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        webViewCallBack.pageError()
    }

    /**
     * url 重定向以及点击页面链接都会执行该方法。
     *
     * @return true: 表示已处理，WebView 不再加载；false: 系统按默认行为继续。
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        webViewCallBack.overrideUrlLoading(view, request)
}
