package com.common.weight.titlebar

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt

object StatusBarUtils {

    private const val SYSTEM_UI_FLAG_OP_STATUS_BAR_TINT = 0x00000010

    fun supportTransparentStatusBar(): Boolean = true

    fun transparentStatusBar(window: Window) {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        window.statusBarColor = Color.TRANSPARENT
    }

    fun setLightMode(window: Window) {
        when {
            OSUtils.isMiui() -> setMIUIStatusBarDarkMode(window, false)
            OSUtils.isFlyme() -> FlymeStatusBarUtils.setStatusBarDarkIcon(window, false)
            OSUtils.isOppo() -> setOppoStatusBarDarkMode(window, false)
            else -> setStatusBarDarkMode(window, false)
        }
    }

    fun setDarkMode(window: Window) {
        when {
            OSUtils.isMiui() -> setMIUIStatusBarDarkMode(window, true)
            OSUtils.isFlyme() -> FlymeStatusBarUtils.setStatusBarDarkIcon(window, true)
            OSUtils.isOppo() -> setOppoStatusBarDarkMode(window, true)
            else -> setStatusBarDarkMode(window, true)
        }
    }

    private fun setStatusBarDarkMode(window: Window, darkMode: Boolean) {
        val decorView = window.decorView ?: return
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = if (darkMode) {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        } else {
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    private fun setMIUIStatusBarDarkMode(window: Window, darkMode: Boolean) {
        // MIUI ≥ 6 dark icon hack only required pre-M; minSdk 24, so just use the standard flag.
        setStatusBarDarkMode(window, darkMode)
    }

    private fun setOppoStatusBarDarkMode(window: Window, darkMode: Boolean) {
        val decorView = window.decorView ?: return
        @Suppress("DEPRECATION")
        var vis = decorView.systemUiVisibility
        vis = if (darkMode) {
            vis or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            vis and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = vis
    }

    fun setStatusBarColor(window: Window, @ColorInt color: Int, alpha: Int) {
        window.statusBarColor = calculateStatusColor(color, alpha)
    }

    private fun calculateStatusColor(@ColorInt color: Int, alpha: Int): Int {
        if (alpha == 0) return color
        val a = 1 - alpha / 255f
        var red = color shr 16 and 0xff
        var green = color shr 8 and 0xff
        var blue = color and 0xff
        red = (red * a + 0.5f).toInt()
        green = (green * a + 0.5f).toInt()
        blue = (blue * a + 0.5f).toInt()
        return 0xff shl 24 or (red shl 16) or (green shl 8) or blue
    }

    fun getStatusBarHeight(context: Context): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) {
            try {
                context.resources.getDimensionPixelSize(resId)
            } catch (e: Resources.NotFoundException) {
                0
            }
        } else {
            0
        }
    }

    fun getNavigationBarHeight(context: Context): Int {
        val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (resId > 0) {
            try {
                context.resources.getDimensionPixelSize(resId)
            } catch (e: Resources.NotFoundException) {
                0
            }
        } else {
            0
        }
    }

    fun checkDeviceHasNavigationBar(context: Context): Boolean {
        var hasNavigationBar = false
        val rs = context.resources
        val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) hasNavigationBar = rs.getBoolean(id)
        runCatching {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val m = systemPropertiesClass.getMethod("get", String::class.java)
            val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as? String
            when (navBarOverride) {
                "1" -> hasNavigationBar = false
                "0" -> hasNavigationBar = true
            }
        }
        return hasNavigationBar
    }

    fun generateViewId(): Int = View.generateViewId()
}
