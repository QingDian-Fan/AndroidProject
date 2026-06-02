package com.common.weight.titlebar

import android.app.Activity
import android.view.View
import android.view.Window
import android.view.WindowManager
import java.lang.reflect.Field
import java.lang.reflect.Method

object FlymeStatusBarUtils {
    private val setStatusBarColorIcon: Method? = runCatching {
        Activity::class.java.getMethod("setStatusBarDarkIcon", Int::class.javaPrimitiveType)
    }.getOrNull()

    private val setStatusBarDarkIconMethod: Method? = runCatching {
        Activity::class.java.getMethod("setStatusBarDarkIcon", Boolean::class.javaPrimitiveType)
    }.getOrNull()

    private val statusBarColorField: Field? = runCatching {
        WindowManager.LayoutParams::class.java.getField("statusBarColor")
    }.getOrNull()

    private val lightStatusBarFlag: Int = runCatching {
        View::class.java.getField("SYSTEM_UI_FLAG_LIGHT_STATUS_BAR").getInt(null)
    }.getOrDefault(0)

    fun isBlackColor(color: Int, level: Int): Boolean = toGrey(color) < level

    fun toGrey(rgb: Int): Int {
        val blue = rgb and 0x000000FF
        val green = (rgb and 0x0000FF00) shr 8
        val red = (rgb and 0x00FF0000) shr 16
        return (red * 38 + green * 75 + blue * 15) shr 7
    }

    fun setStatusBarDarkIcon(activity: Activity, color: Int) {
        val method = setStatusBarColorIcon
        if (method != null) {
            runCatching { method.invoke(activity, color) }
            return
        }
        val whiteColor = isBlackColor(color, 50)
        if (statusBarColorField != null) {
            setStatusBarDarkIcon(activity, whiteColor, true)
            setStatusBarDarkIcon(activity.window, color)
        } else {
            setStatusBarDarkIcon(activity, whiteColor)
        }
    }

    fun setStatusBarDarkIcon(window: Window, color: Int) {
        runCatching {
            setStatusBarColor(window, color)
            setStatusBarDarkIcon(window.decorView, true)
        }
    }

    fun setStatusBarDarkIcon(activity: Activity, dark: Boolean) {
        setStatusBarDarkIcon(activity, dark, true)
    }

    fun setStatusBarDarkIcon(window: Window, dark: Boolean) {
        val decorView = window.decorView ?: return
        setStatusBarDarkIcon(decorView, dark)
        setStatusBarColor(window, 0)
    }

    private fun setStatusBarDarkIcon(view: View, dark: Boolean) {
        @Suppress("DEPRECATION")
        val oldVis = view.systemUiVisibility
        val newVis = if (dark) oldVis or lightStatusBarFlag else oldVis and lightStatusBarFlag.inv()
        if (newVis != oldVis) {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = newVis
        }
    }

    private fun setStatusBarColor(window: Window, color: Int) {
        val field = statusBarColorField ?: return
        val winParams = window.attributes
        runCatching {
            val oldColor = field.getInt(winParams)
            if (oldColor != color) {
                field.set(winParams, color)
                window.attributes = winParams
            }
        }
    }

    private fun setStatusBarDarkIcon(activity: Activity, dark: Boolean, flag: Boolean) {
        val method = setStatusBarDarkIconMethod
        if (method != null) {
            runCatching { method.invoke(activity, dark) }
        } else if (flag) {
            setStatusBarDarkIcon(activity.window, dark)
        }
    }
}