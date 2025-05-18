package com.dian.demo.utils.webview.webset

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.dian.demo.utils.webview.callback.LoadProgressCallBack
import com.dian.demo.utils.webview.callback.WebViewCallBack


class DefaultWebChromeClient(private val webViewCallBack: WebViewCallBack) : WebChromeClient() {

    private var progressCallBack: LoadProgressCallBack? = null

    fun setLoadProgressCallBack(progressCallBack: LoadProgressCallBack) {
        this.progressCallBack = progressCallBack
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        webViewCallBack.updateTitle(title)
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        if (progressCallBack != null) {
            progressCallBack!!.onCurrentProgress(newProgress)
        }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        Log.d("QWER", "onConsoleMessage: ${consoleMessage?.message()}")
        return super.onConsoleMessage(consoleMessage)
    }


    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        return super.onJsAlert(view, url, message, result)
    }
}