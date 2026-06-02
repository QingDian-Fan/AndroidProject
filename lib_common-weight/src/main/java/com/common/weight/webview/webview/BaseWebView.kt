package com.common.weight.webview.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import com.common.utils.LogUtil
import com.common.weight.webview.bean.JsParam
import com.common.weight.webview.callback.WebViewCallBack
import com.common.weight.webview.dispatcher.WebCommandDispatcher
import com.common.weight.webview.webset.DefaultWebChromeClient
import com.common.weight.webview.webset.DefaultWebSetting
import com.common.weight.webview.webset.DefaultWebViewClient
import com.google.gson.Gson
import org.json.JSONObject
import java.io.File

/**
 * 基础 WebView：
 *
 *  - 已实现 [NestedScrollingChild]，可与 `CoordinatorLayout` / 自定义 NestedScroll 容器联动；
 *  - 默认应用 [DefaultWebSetting]；
 *  - 默认注入 `window.webview.takeNativeAction(json)` JS Bridge，由
 *    [WebCommandDispatcher] 派发到具体 [com.common.weight.webview.command.Command]。
 *
 * 参考: https://juejin.cn/post/7049735291778629645
 */
open class BaseWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.webViewStyle
) : WebView(context, attrs, defStyleAttr), NestedScrollingChild {

    private val childHelper = NestedScrollingChildHelper(this)

    private var lastMotionY = 0
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private var nestedOffsetY = 0
    private var cancelDispatched = false

    private var chromeClient: DefaultWebChromeClient? = null
    private val gson: Gson by lazy { Gson() }

    init {
        isNestedScrollingEnabled = true
        DefaultWebSetting.apply(this)
        addJavascriptInterface(this, JS_INTERFACE_NAME)
    }

    fun initWebClient(webViewCallBack: WebViewCallBack) {
        webViewClient = DefaultWebViewClient(webViewCallBack)
        chromeClient = DefaultWebChromeClient(webViewCallBack).also { webChromeClient = it }
    }

    fun getChromeClient(): DefaultWebChromeClient? = chromeClient

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var result = false
        val trackedEvent = MotionEvent.obtain(event)
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) nestedOffsetY = 0

        val y = event.y.toInt()
        event.offsetLocation(0f, nestedOffsetY.toFloat())
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                cancelDispatched = false
                lastMotionY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                result = super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaY = lastMotionY - y
                if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset)) {
                    deltaY -= scrollConsumed[1]
                    trackedEvent.offsetLocation(0f, scrollOffset[1].toFloat())
                    nestedOffsetY += scrollOffset[1]
                }
                lastMotionY = y - scrollOffset[1]
                val oldY = scrollY
                val newScrollY = (oldY + deltaY).coerceAtLeast(0)
                val dyConsumed = newScrollY - oldY
                val dyUnconsumed = deltaY - dyConsumed
                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, scrollOffset)) {
                    lastMotionY -= scrollOffset[1]
                    trackedEvent.offsetLocation(0f, scrollOffset[1].toFloat())
                    nestedOffsetY += scrollOffset[1]
                }
                if (scrollConsumed[1] == 0 && scrollOffset[1] == 0) {
                    if (cancelDispatched) {
                        cancelDispatched = false
                        trackedEvent.action = MotionEvent.ACTION_DOWN
                        super.onTouchEvent(trackedEvent)
                    } else {
                        result = super.onTouchEvent(trackedEvent)
                    }
                    trackedEvent.recycle()
                } else if (!cancelDispatched) {
                    cancelDispatched = true
                    super.onTouchEvent(
                        MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
                    )
                }
            }
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll()
                result = super.onTouchEvent(event)
            }
        }
        return result
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        childHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean = childHelper.isNestedScrollingEnabled

    override fun startNestedScroll(axes: Int): Boolean = childHelper.startNestedScroll(axes)

    override fun stopNestedScroll() {
        childHelper.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean = childHelper.hasNestedScrollingParent()

    override fun dispatchNestedScroll(
        dxConsumed: Int, dyConsumed: Int,
        dxUnconsumed: Int, dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedScroll(
        dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow
    )

    override fun dispatchNestedPreScroll(
        dx: Int, dy: Int,
        consumed: IntArray?, offsetInWindow: IntArray?
    ): Boolean = childHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean): Boolean =
        childHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean =
        childHelper.dispatchNestedPreFling(velocityX, velocityY)

    // ---------- JS Bridge ----------

    /**
     * H5 调用入口：
     *
     * ```js
     * window.webview.takeNativeAction(JSON.stringify({ name: "showToast", param: { message: "Hi" } }));
     * ```
     */
    @JavascriptInterface
    fun takeNativeAction(jsParam: String?) {
        LogUtil.e(TAG, "takeNativeAction: $jsParam")
        if (jsParam.isNullOrEmpty()) return
        val payload = runCatching { gson.fromJson(jsParam, JsParam::class.java) }
            .onFailure { LogUtil.e(TAG, "parse JsParam failed: ${it.message}") }
            .getOrNull() ?: return
        WebCommandDispatcher.executeCommand(payload.name, gson.toJson(payload.param), this)
    }

    /**
     * 回灌结果给 H5。`response` 通过 [JSONObject.quote] 转义，避免拼接 JS 时被换行/引号截断。
     */
    fun handleCallback(callbackName: String, response: String?) {
        if (callbackName.isEmpty()) return
        val safeName = JSONObject.quote(callbackName)
        // response 约定是合法 JSON 字符串；为空时给一个 null，避免拼出非法 JS。
        val safeResponse = response?.takeIf { it.isNotEmpty() } ?: "null"
        post {
            val jsCode = "javascript:demojs.callback($safeName,$safeResponse)"
            LogUtil.e(TAG, "handleCallback: $jsCode")
            evaluateJavascript(jsCode, null)
        }
    }

    /**
     * 仅清理 WebView 自身产生的数据：Cookie / 历史 / 表单 / Storage / ServiceWorker /
     * `app_webview` 目录。
     *
     * 旧实现额外 `deleteRecursively()` 了应用的 cacheDir、codeCacheDir、externalCacheDir，
     * 这些目录是整个 App 共享的，会误删图片缓存、临时文件等；本实现不再触碰它们。
     */
    fun clearWebViewData(context: Context) {
        runCatching {
            // Cookie
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()

            // 当前 WebView 实例
            clearCache(true)
            clearHistory()
            clearFormData()

            // Storage
            WebStorage.getInstance().deleteAllData()

            // ServiceWorker（minSdk 24，恒满足 N+）
            ServiceWorkerController.getInstance().apply {
                serviceWorkerWebSettings.setBlockNetworkLoads(true)
                setServiceWorkerClient(object : ServiceWorkerClient() {})
            }

            // WebView 私有目录
            runCatching {
                val webviewDir = File(context.dataDir, "app_webview")
                if (webviewDir.exists()) webviewDir.deleteRecursively()
            }
            runCatching { context.deleteSharedPreferences("WebViewPrefs") }
        }.onFailure { it.printStackTrace() }
    }

    private companion object {
        const val TAG = "WebView"
        const val JS_INTERFACE_NAME = "webview"
    }
}
