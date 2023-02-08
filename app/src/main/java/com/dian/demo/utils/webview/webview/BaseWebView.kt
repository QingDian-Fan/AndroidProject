package com.dian.demo.utils.webview.webview

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebView
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

/**
 * @ClassName: BaseWebView
 * @Description: java类作用描述
 * @Author: liyihuan
 * @Date: 2021/7/15 22:09
 */
class BaseWebView : WebView, NestedScrollingChild {

    private var mChildHelper: NestedScrollingChildHelper? = null

    private var mLastMotionY = 0

    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)

    private var mNestedOffsetY = 0
    private var mChange = false

    constructor(context: Context): super(context)

    constructor(context: Context, attributeSet: AttributeSet): super(context, attributeSet)

    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int): super(
        context,
        attributeSet,
        defStyleAttr
    )
    init {
        mChildHelper = NestedScrollingChildHelper(this)
        isNestedScrollingEnabled = true
    }

    private var mWebChromeClient: DefaultWebChromeClient?=null
    init {
        WebCommandDispatcher.instance.initAidlConnection()
        DefaultWebSetting.getSetting(this)
        addJavascriptInterface(this, "android_web_view")
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
                val newScrollY = Math.max(0, oldY + deltaY)
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
                val jscode = "javascript:myjs.callback('$callbackname',$response)"
                LogUtil.e("xxxxxx", jscode)
                evaluateJavascript(jscode, null)
            }
        }
    }
}