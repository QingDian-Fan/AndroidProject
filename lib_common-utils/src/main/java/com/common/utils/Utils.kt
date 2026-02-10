package com.common.utils

import android.content.Context
import android.os.Build
import android.text.TextUtils
import com.common.theme.BaseApplication
import com.meituan.android.walle.WalleChannelReader

object Utils {

    var channel: String? = null
    fun getChannel(context: Context= BaseApplication.getAppContext()): String {
        if (channel.isNullOrBlank()) {
            channel = WalleChannelReader.getChannel(context) ?: "official"
        }
        return channel!!
    }

    var isDebug = false
        private set

    fun init(debug: Boolean) {
        isDebug = debug
    }

    fun getApplicationId(): String {
        return BaseApplication.getAppContext().applicationContext.packageName
    }


    fun getVersionName(context: Context = BaseApplication.getAppContext()): String =
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: ""
        } catch (e: Exception) {
            ""
        }

    fun getVersionCode(context: Context = BaseApplication.getAppContext()): Long =
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