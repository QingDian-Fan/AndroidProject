package com.dian.demo.utils.ext

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import com.dian.demo.utils.LogUtil


fun View?.setPadding(px: Int) = this?.run {
    setPadding(px, px, px, px)
}

fun View?.visible() {
    this?.visibility = View.VISIBLE
}

fun View?.gone() {
    this?.visibility = View.GONE
}

fun View?.invisible() {
    this?.visibility = View.INVISIBLE
}

fun View?.onPreDraw(block: View.() -> Unit) = this?.run {
    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (viewTreeObserver.isAlive) {
                viewTreeObserver.removeOnPreDrawListener(this)
            }
            block.invoke(this@run)
            return true
        }
    })
}

fun View?.measureSize(): IntArray? {
    this?.apply {
        var lp: ViewGroup.LayoutParams? = layoutParams
        if (lp == null) {
            lp = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val widthSpec = ViewGroup.getChildMeasureSpec(0, 0, lp.width)
        val lpHeight = lp.height
        val heightSpec: Int
        heightSpec = if (lpHeight > 0) {
            View.MeasureSpec.makeMeasureSpec(lpHeight, View.MeasureSpec.EXACTLY)
        } else {
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        }
        measure(widthSpec, heightSpec)
        return intArrayOf(measuredWidth, measuredHeight)
    }
    return null
}


fun <T : View> T.click(action: (T) -> Unit) {
    setOnClickListener {
        action(this)
    }
}

fun <T : View> T.longClick(action: (T) -> Boolean) {
    setOnLongClickListener {
        action(this)
    }
}

/**
 * 带有限制快速点击的点击事件
 */
fun <T : View> T.singleClick(interval: Long = 500L, action: ((T) -> Unit)?) {
    setOnClickListener(SingleClickListener(interval, action))
}

private var lastClickTime = 0L
private var mView: View? = null


class SingleClickListener<T : View>(
    private val interval: Long = 500L,
    private var clickFunc: ((T) -> Unit)?
) : View.OnClickListener {

    override fun onClick(v: View) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastClickTime > interval || mView == null || v != mView) {
            // 单次点击事件
            clickFunc?.invoke(v as T)
            lastClickTime = nowTime
            mView = v
        }
    }
}