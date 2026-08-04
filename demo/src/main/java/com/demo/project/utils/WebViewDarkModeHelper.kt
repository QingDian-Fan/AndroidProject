package com.demo.project.utils

import android.content.Context
import android.content.res.Configuration
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatDelegate
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * WebView 暗夜模式辅助类
 * - 支持强制日间 / 夜间 / 跟随系统
 * - 自动同步 WebView prefers-color-scheme
 * - Android 13+ 自动生效，无需 JS 注入
 * - 旧版 WebView 自动注入暗夜 CSS
 */
object WebViewDarkModeHelper {

    enum class Mode {
        /** 跟随系统 */
        FOLLOW_SYSTEM,

        /** 强制日间 */
        LIGHT,

        /** 强制夜间 */
        DARK
    }

    /**
     * 应用全局主题（AppCompat + WebView）
     */
    fun apply(context: Context, webView: WebView, mode: Mode = Mode.FOLLOW_SYSTEM) {
        val settings = webView.settings
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.javaScriptEnabled = true

        // 设置全局 AppCompatDelegate 模式
        when (mode) {
            Mode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Mode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Mode.FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        // 当前是否暗色模式
        val isDarkMode = when (mode) {
            Mode.DARK -> true
            Mode.LIGHT -> false
            Mode.FOLLOW_SYSTEM -> {
                val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                uiMode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        // 优先使用新特性 AlgorithmicDarkening（Android 13+）
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
        } else if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            //向下兼容旧版本
            WebSettingsCompat.setForceDark(
                settings,
                if (isDarkMode) WebSettingsCompat.FORCE_DARK_ON
                else WebSettingsCompat.FORCE_DARK_OFF
            )
        } else {
            //最老的 WebView，不支持暗化：注入 CSS
            injectDarkModeCSS(webView, isDarkMode)
        }
    }

    /**
     * 兼容旧 WebView 注入暗夜 CSS
     */
    private fun injectDarkModeCSS(webView: WebView, enable: Boolean) {
        if (!enable) {
            val removeJs = """
                var existingStyle = document.getElementById('dark-mode-style');
                if (existingStyle) existingStyle.remove();
            """.trimIndent()
            webView.evaluateJavascript(removeJs, null)
            return
        }

        val css = """
            html, body {
                background-color: #121212 !important;
                color: #E0E0E0 !important;
            }
            a { color: #BB86FC !important; }
            img, video { filter: brightness(0.8) contrast(1.1); }
        """.trimIndent()

        val js = """
            var style = document.createElement('style');
            style.id = 'dark-mode-style';
            style.type = 'text/css';
            style.appendChild(document.createTextNode(`$css`));
            document.head.appendChild(style);
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    /**
     * 当系统主题变化时调用（例如配置变化).
     */
    fun onSystemThemeChanged(context: Context, webView: WebView) {
        apply(context, webView, Mode.FOLLOW_SYSTEM)
    }
}