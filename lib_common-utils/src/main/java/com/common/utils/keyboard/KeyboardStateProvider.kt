package com.common.utils.keyboard

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupWindow

internal class KeyboardStateProvider(private val mActivity: Activity) : PopupWindow(mActivity) {

    private var mOnKeyboardStateChangeListener: OnKeyboardStateChangeListener? = null
    private val mPopupView: View
    private val mParentView: View
    private var mKeyboardLandscapeHeight = 0
    private var mKeyboardPortraitHeight = 0

    init {
        mActivity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        mPopupView = LinearLayout(mActivity)
        contentView = mPopupView
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        inputMethodMode = INPUT_METHOD_NEEDED
        mParentView = mActivity.window.decorView
        width = 0
        height = WindowManager.LayoutParams.MATCH_PARENT
        mPopupView.viewTreeObserver.addOnGlobalLayoutListener { handleOnGlobalLayout() }
    }

    fun start() {
        mParentView.post {
            if (!isShowing && mParentView.windowToken != null) {
                setBackgroundDrawable(ColorDrawable(0))
                showAtLocation(mParentView, Gravity.NO_GRAVITY, 0, 0)
            }
        }
    }

    fun close() {
        mOnKeyboardStateChangeListener = null
        dismiss()
    }

    fun setKeyboardHeightObserver(observer: OnKeyboardStateChangeListener?) {
        mOnKeyboardStateChangeListener = observer
    }

    private fun getScreenOrientation(): Int {
        return mActivity.resources.configuration.orientation
    }

    private fun handleOnGlobalLayout() {
        val screenSize = Point()
        mActivity.windowManager.defaultDisplay.getSize(screenSize)
        val rect = Rect()
        mPopupView.getWindowVisibleDisplayFrame(rect)
        val orientation = getScreenOrientation()
        val keyboardHeight = screenSize.y - rect.bottom
        if (keyboardHeight == 0) {
            notifyKeyboardStateChanged(false, 0, orientation)
        } else {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                mKeyboardPortraitHeight = keyboardHeight
                notifyKeyboardStateChanged(true, mKeyboardPortraitHeight, orientation)
            } else {
                mKeyboardLandscapeHeight = keyboardHeight
                notifyKeyboardStateChanged(true, mKeyboardLandscapeHeight, orientation)
            }
        }
    }

    private fun notifyKeyboardStateChanged(isShow: Boolean, height: Int, orientation: Int) {
        mOnKeyboardStateChangeListener?.onKeyboardStateChanged(isShow, height, orientation)
    }
}