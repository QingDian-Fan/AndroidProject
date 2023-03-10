package com.dian.demo.utils.webview.webset

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.dian.demo.utils.webview.callback.WebViewCallBack


class DefaultWebViewClient(private val webViewCallBack: WebViewCallBack) : WebViewClient() {


    override fun onPageFinished(view: WebView?, url: String?) {
        webViewCallBack.pageFinished(url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        webViewCallBack.pageStarted(url)
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        webViewCallBack.pageError()
    }

    /**
     * url重定向会执行此方法以及点击页面某些链接也会执行此方法
     *
     * @return true:表示当前url已经加载完成，即使url还会重定向都不会再进行加载
     *          false 表示此url默认由系统处理，该重定向还是重定向，直到加载完成
     */
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        return webViewCallBack.overrideUrlLoading(view,request)
    }

    // WebView发生改变时调用
    override fun onScaleChanged(view: WebView?, oldScale: Float, newScale: Float) {
        super.onScaleChanged(view, oldScale, newScale)
    }


}