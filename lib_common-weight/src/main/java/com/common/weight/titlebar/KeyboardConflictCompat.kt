package com.common.weight.titlebar

import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import android.widget.FrameLayout

/**
 * 解决沉浸式标题栏下，键盘兼容问题
 */
class KeyboardConflictCompat private constructor(window: Window) {

    private val childOfContent: View
    private val frameLayoutParams: FrameLayout.LayoutParams
    private val statusBarHeight: Int
    private var usableHeightPrevious = 0
    private var contentHeight = 0
    private var firstLayout = true

    private val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        if (firstLayout) {
            contentHeight = childOfContent.height
            firstLayout = false
        }
        possiblyResizeChildOfContent()
    }

    private val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {
            v.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
            v.removeOnAttachStateChangeListener(this)
        }
    }

    init {
        val content = window.findViewById<FrameLayout>(android.R.id.content)
            ?: error("Window has no content view")
        childOfContent = content.getChildAt(0)
            ?: error("Window content has no child")
        frameLayoutParams = childOfContent.layoutParams as? FrameLayout.LayoutParams
            ?: error("Window content child must use FrameLayout.LayoutParams")
        statusBarHeight = StatusBarUtils.getStatusBarHeight(window.context)
        childOfContent.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
        childOfContent.addOnAttachStateChangeListener(attachStateListener)
    }

    private fun possiblyResizeChildOfContent() {
        val usableHeightNow = computeUsableHeight()
        if (usableHeightNow == usableHeightPrevious) return

        val usableHeightSansKeyboard = childOfContent.rootView.height
        val heightDifference = usableHeightSansKeyboard - usableHeightNow
        frameLayoutParams.height = if (heightDifference > usableHeightSansKeyboard / 4) {
            usableHeightSansKeyboard - heightDifference + statusBarHeight
        } else {
            contentHeight
        }
        childOfContent.requestLayout()
        usableHeightPrevious = usableHeightNow
    }

    private fun computeUsableHeight(): Int {
        val r = Rect()
        childOfContent.getWindowVisibleDisplayFrame(r)
        return r.bottom - r.top
    }

    companion object {
        @JvmStatic
        fun assistWindow(window: Window) {
            KeyboardConflictCompat(window)
        }
    }
}
