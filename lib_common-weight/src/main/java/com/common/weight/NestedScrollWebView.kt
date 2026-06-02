package com.common.weight

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild3
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat

class NestedScrollWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : WebView(context, attrs), NestedScrollingChild3 {

    private val childHelper = NestedScrollingChildHelper(this)
    private val scrollConsumed = IntArray(2)
    private val scrollOffset = IntArray(2)

    init {
        isNestedScrollingEnabled = true
        childHelper.isNestedScrollingEnabled = true
    }

    // ===== 核心：拦截 scroll，让 WebView 先消耗 =====
    override fun onScrollChanged(
        l: Int,
        t: Int,
        oldl: Int,
        oldt: Int
    ) {
        super.onScrollChanged(l, t, oldl, oldt)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        if (action == MotionEvent.ACTION_DOWN) {
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            stopNestedScroll(ViewCompat.TYPE_TOUCH)
        }

        return super.onTouchEvent(event)
    }

    // ===== 关键：分发 Nested Scroll =====
    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {

        // 只有「WebView 已经到最底 + 继续向上滑」时，才允许父布局预消费
        val allowParent =
            dy > 0 && !canScrollVertically(1)

        return if (allowParent) {
            childHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow, type
            )
        } else {
            false
        }
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
        consumed: IntArray
    ) {
        // 把 WebView 吃不掉的惯性滑动交给 BottomSheet
        if (dyUnconsumed > 0 && !canScrollVertically(1)) {
            childHelper.dispatchNestedScroll(
                dxConsumed,
                dyConsumed,
                dxUnconsumed,
                dyUnconsumed,
                offsetInWindow,
                type,
                consumed
            )
        }
    }

    // ===== NestedScrollingChild3 =====
    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return childHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll(type: Int) {
        childHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return childHelper.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int,
    ): Boolean {
        if (dyUnconsumed > 0 && !canScrollVertically(1)) {
            return childHelper.dispatchNestedScroll(
                dxConsumed,
                dyConsumed,
                dxUnconsumed,
                dyUnconsumed,
                offsetInWindow,
                type
            )
        }
        return false
    }

}

