package com.common.weight.webview.webset

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * 一组通用的 WebView 默认配置。改写自原 `object DefaultWebSetting`：
 *
 *  - 原实现持有 `lateinit var mWebSettings: WebSettings`，多个 WebView 共用一个全局
 *    引用，会被后初始化的 WebView 覆盖；这里改为无状态函数；
 *  - 去掉 `Build.VERSION_CODES.JELLY_BEAN / KITKAT` 等 minSdk 24 已恒真的分支；
 *  - 去掉了 `databasePath`（API 19+ 已废弃且无用）和重复的 `domStorageEnabled = true`。
 */
object DefaultWebSetting {

    @SuppressLint("SetJavaScriptEnabled")
    @JvmStatic
    fun apply(webView: WebView): WebSettings = webView.settings.apply {
        // JavaScript
        javaScriptEnabled = true
        javaScriptCanOpenWindowsAutomatically = true

        // 缩放
        setSupportZoom(true)
        builtInZoomControls = false

        // 缓存与存储
        cacheMode = WebSettings.LOAD_DEFAULT
        databaseEnabled = true
        domStorageEnabled = true

        // 图片
        loadsImagesAutomatically = true
        blockNetworkImage = false

        // 文件
        allowFileAccess = true
        // 通过 file:// 加载的 JS 不允许访问其他源（安全考虑）
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false

        // 隐私
        savePassword = false
        saveFormData = false

        // 视图与字体
        loadWithOverviewMode = true
        useWideViewPort = true
        textZoom = 100
        defaultFontSize = 16
        minimumFontSize = 10
        defaultTextEncodingName = "utf-8"

        // 多窗口 & 定位
        setSupportMultipleWindows(false)
        setGeolocationEnabled(true)
        setNeedInitialFocus(true)

        // 布局算法（minSdk 24，KITKAT 已恒真）
        layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN

        // 应用缓存目录（保留作为路径设置参考，等价于原始实现）
        val cacheDir = webView.context.getDir("cache", Context.MODE_PRIVATE).path
        @Suppress("DEPRECATION")
        databasePath = cacheDir
    }
}
