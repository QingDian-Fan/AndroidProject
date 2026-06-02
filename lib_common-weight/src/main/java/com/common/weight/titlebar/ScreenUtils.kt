package com.common.weight.titlebar

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.DisplayMetrics
import android.view.View
import android.view.inputmethod.InputMethodManager

object ScreenUtils {

    fun dp2Px(context: Context?, dp: Float): Float {
        if (context == null) return -1f
        return dp * density(context)
    }

    fun px2Dp(context: Context?, px: Float): Float {
        if (context == null) return -1f
        return px / density(context)
    }

    fun density(context: Context): Float =
        context.resources.displayMetrics.density

    fun dp2PxInt(context: Context, dp: Float): Int =
        (dp2Px(context, dp) + 0.5f).toInt()

    fun px2DpCeilInt(context: Context, px: Float): Float =
        (px2Dp(context, px) + 0.5f).toInt().toFloat()

    fun sp2px(context: Context, spValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (spValue * fontScale + 0.5f).toInt()
    }

    fun px2sp(context: Context, pxValue: Float): Int {
        val fontScale = context.resources.displayMetrics.scaledDensity
        return (pxValue / fontScale + 0.5f).toInt()
    }

    fun getDisplayMetrics(context: Context): DisplayMetrics {
        val activity = context.findActivity()
        return if (activity != null) {
            DisplayMetrics().also { activity.windowManager.defaultDisplay.getMetrics(it) }
        } else {
            context.resources.displayMetrics
        }
    }

    fun getScreenPixelSize(context: Context): IntArray {
        val metrics = getDisplayMetrics(context)
        return intArrayOf(metrics.widthPixels, metrics.heightPixels)
    }

    fun hideSoftInputKeyBoard(context: Context, focusView: View?) {
        if (focusView == null) return
        val binder = focusView.windowToken ?: return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.hideSoftInputFromWindow(binder, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun showSoftInputKeyBoard(context: Context, focusView: View?) {
        if (focusView == null) return
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return
        imm.showSoftInput(focusView, InputMethodManager.SHOW_FORCED)
    }

    fun getScreenWidth(context: Context): Int =
        context.resources.displayMetrics.widthPixels

    fun getScreenHeight(context: Context): Int =
        context.resources.displayMetrics.heightPixels

    fun getStatusBarHeight(context: Context): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }

    fun getAppInScreenheight(context: Context): Int =
        getScreenHeight(context) - getStatusBarHeight(context)

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext?.findActivity()
        else -> null
    }
}
