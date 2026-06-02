package com.common.weight.webview.webset

import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.common.utils.LogUtil
import com.common.weight.webview.callback.LoadProgressCallBack
import com.common.weight.webview.callback.WebViewCallBack

/**
 * 默认 `WebChromeClient`：把标题、加载进度、控制台输出统一转给上层。
 */
class DefaultWebChromeClient(
    private val webViewCallBack: WebViewCallBack
) : WebChromeClient() {

    var progressCallBack: LoadProgressCallBack? = null

    fun setLoadProgressCallBack(progressCallBack: LoadProgressCallBack?) {
        this.progressCallBack = progressCallBack
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        webViewCallBack.updateTitle(title)
    }

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressCallBack?.onCurrentProgress(newProgress)
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
            LogUtil.d(TAG, "console[${it.messageLevel()}] ${it.message()} @${it.sourceId()}:${it.lineNumber()}")
        }
        return super.onConsoleMessage(consoleMessage)
    }

    private companion object {
        const val TAG = "WebView"
    }
}
