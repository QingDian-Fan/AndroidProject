package com.dian.demo.utils.webview.webview

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.ServiceWorkerClient
import android.webkit.ServiceWorkerController
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.core.view.NestedScrollingChild
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import com.dian.demo.http.gson.GsonFactory
import com.dian.demo.utils.webview.WebCommandDispatcher
import com.dian.demo.utils.webview.bean.JsParam
import com.dian.demo.utils.webview.callback.WebViewCallBack
import com.dian.demo.utils.webview.webset.DefaultWebChromeClient
import com.dian.demo.utils.webview.webset.DefaultWebSetting
import com.dian.demo.utils.webview.webset.DefaultWebViewClient
import com.dian.demo.utils.LogUtil
import java.io.File

/**
 * https://juejin.cn/post/7049735291778629645
 */
open class BaseWebView : WebView, NestedScrollingChild {

    private var mChildHelper: NestedScrollingChildHelper? = null

    private var mLastMotionY = 0

    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)

    private var mNestedOffsetY = 0
    private var mChange = false

    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet?): super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int): super(
        context,
        attributeSet,
        defStyleAttr
    )


    private var mWebChromeClient: DefaultWebChromeClient?=null
    init {
        mChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
        WebCommandDispatcher.instance.initAidlConnection()
        DefaultWebSetting.getSetting(this)
        addJavascriptInterface(this, "webview")
    }

    fun initWebClient(webViewCallBack: WebViewCallBack) {
        webViewClient = DefaultWebViewClient(webViewCallBack)
        mWebChromeClient = DefaultWebChromeClient(webViewCallBack)
        webChromeClient = mWebChromeClient
    }

    fun getChromeClient(): DefaultWebChromeClient?{
        return mWebChromeClient
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        var result = false
        val trackedEvent = MotionEvent.obtain(event)
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0
        }
        val y = event.y.toInt()
        event.offsetLocation(0f, mNestedOffsetY.toFloat())
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mChange = false
                mLastMotionY = y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
                result = super.onTouchEvent(event)
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaY = mLastMotionY - y
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1]
                    trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedOffsetY += mScrollOffset[1]
                }
                mLastMotionY = y - mScrollOffset[1]
                val oldY = scrollY
                val newScrollY = 0.coerceAtLeast(oldY + deltaY)
                val dyConsumed = newScrollY - oldY
                val dyUnconsumed = deltaY - dyConsumed
                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, mScrollOffset)) {
                    mLastMotionY -= mScrollOffset[1]
                    trackedEvent.offsetLocation(0f, mScrollOffset[1].toFloat())
                    mNestedOffsetY += mScrollOffset[1]
                }
                if (mScrollConsumed[1] == 0 && mScrollOffset[1] == 0) {
                    if (mChange) {
                        mChange = false
                        trackedEvent.action = MotionEvent.ACTION_DOWN
                        super.onTouchEvent(trackedEvent)
                    } else {
                        result = super.onTouchEvent(trackedEvent)
                    }
                    trackedEvent.recycle()
                } else {
                    if (!mChange) {
                        mChange = true
                        super.onTouchEvent(
                            MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
                        )
                    }
                }
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopNestedScroll()
                result = super.onTouchEvent(event)
            }
            else -> {
            }
        }
        return result
    }
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper!!.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return mChildHelper!!.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return mChildHelper!!.startNestedScroll(axes)
    }

    override fun stopNestedScroll() {
        mChildHelper!!.stopNestedScroll()
    }

    override fun hasNestedScrollingParent(): Boolean {
        return mChildHelper!!.hasNestedScrollingParent()
    }

    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return mChildHelper!!.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?, offsetInWindow: IntArray?): Boolean {
        return mChildHelper!!.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean
    ): Boolean {
        return mChildHelper!!.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return mChildHelper!!.dispatchNestedPreFling(velocityX, velocityY)
    }

    @JavascriptInterface
    fun takeNativeAction(jsParam: String?) {
        LogUtil.e("TAG--->WebView","jsParam:$jsParam")
        if (!TextUtils.isEmpty(jsParam)) {
            val jsParamObject: JsParam = GsonFactory.getSingletonGson().fromJson(
                jsParam,
                JsParam::class.java
            )
            WebCommandDispatcher.instance.executeCommand(
                jsParamObject.name, GsonFactory.getSingletonGson().toJson(
                    jsParamObject.param
                ), this
            )
        }
    }



    fun handleCallback(callbackname: String, response: String?) {
        if (!TextUtils.isEmpty(callbackname) && !TextUtils.isEmpty(response)) {
            post {
                val jscode = "javascript:demojs.callback('$callbackname',$response)"
                LogUtil.e("xxxxxx", jscode)
                evaluateJavascript(jscode, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun clearWebViewData(context: Context) {
        try {

            // ============= 1. 清除 Cookie（最关键） =============
            val cookieManager = CookieManager.getInstance()
            cookieManager.removeAllCookies(null)
            cookieManager.flush()   // 必须，否则不会真正删除

            // ============= 2. 清除 WebView 缓存/历史/FormData ============
            WebView(context).apply {
                clearCache(true)     // 删除缓存文件
                clearHistory()       // 删除访问记录
                clearFormData()      // 删除表单自动填充
            }

            // ============= 3. 清 WebStorage：LocalStorage / Web SQL ============
            WebStorage.getInstance().deleteAllData()

            // ============= 4. 清除 ServiceWorker 缓存（Android 7.0+） ============
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val controller = ServiceWorkerController.getInstance()
                controller.serviceWorkerWebSettings.setBlockNetworkLoads(true)
                controller.setServiceWorkerClient(object : ServiceWorkerClient() {})
            }

            // ============= 5. 删除 WebView 相关缓存目录 ============

            // /data/data/xxx/cache/
            context.cacheDir?.deleteRecursively()

            // /data/data/xxx/code_cache/
            context.codeCacheDir?.deleteRecursively()

            // /storage/emulated/0/Android/data/xxx/cache/
            context.externalCacheDir?.deleteRecursively()

            // WebView 的内部目录：/data/data/xxx/app_webview
            try {
                val webviewDir = File(context.dataDir, "app_webview")
                if (webviewDir.exists()) {
                    webviewDir.deleteRecursively()
                }
            } catch (_: Exception) { }

            // ============= 6. 删除 WebView 的 SharedPreferences ============
            try {
                context.deleteSharedPreferences("WebViewPrefs")
            } catch (_: Exception) { }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}