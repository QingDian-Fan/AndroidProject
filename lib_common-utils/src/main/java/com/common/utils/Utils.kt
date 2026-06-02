package com.common.utils

import android.app.Application
import android.content.Context
import android.os.Build
import android.text.TextUtils
import com.common.theme.BaseApplication
import com.meituan.android.walle.WalleChannelReader

object Utils {

    var channel: String? = null
    fun getChannel(context: Context= getAppContext()): String {
        if (channel.isNullOrBlank()) {
            channel = WalleChannelReader.getChannel(context) ?: "official"
        }
        return channel.orEmpty().ifBlank { "official" }
    }
    private lateinit var appContext: Context
    private lateinit var mApplication: Application

    var isDebug = false
        private set

    fun init(context: Application,debug: Boolean) {
        mApplication = context
        appContext = context.applicationContext
        isDebug = debug
    }

    fun getApplicationId(): String {
        return getAppContext().applicationContext.packageName
    }

    fun getAppContext(): Context {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("Utils not initialized. Call Utils.init(context) first.")
        }
        return appContext
    }

    @JvmStatic
    fun getAppInstance(): Context {
        if (!::mApplication.isInitialized) {
            throw IllegalStateException("Utils not initialized. Call Utils.init(context) first.")
        }
        return mApplication
    }


    fun getVersionName(context: Context = getAppContext()): String =
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: ""
        } catch (e: Exception) {
            ""
        }


    fun getVersionCode(context: Context = getAppContext()): Long =
        try {
            val info = context.packageManager
                .getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (e: Exception) {
            1
        }

}
