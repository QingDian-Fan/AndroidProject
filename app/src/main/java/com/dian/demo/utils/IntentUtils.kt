package com.dian.demo.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.annotation.RequiresPermission

object IntentUtils {

    /**
     * 调用系统拨号界面
     */
    fun dialingPhone(context: Context, phoneNum: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        val data = Uri.parse("tel:$phoneNum")
        intent.data = data
        context.startActivity(intent)
    }

    /**
     * 调用系统拨号界面
     */
    @RequiresPermission(value = "android.permission.CALL_PHONE")
    fun callPhone(context: Context, phoneNum: String) {
        val intent = Intent(Intent.ACTION_CALL)
        val data = Uri.parse("tel:$phoneNum")
        intent.data = data
        context.startActivity(intent)
    }

    /**
     * 应用设置
     */
    fun goToSetting(context: Context) {
        val intent = Intent(Settings.ACTION_SETTINGS)
        context.startActivity(intent)
    }

    /**
     * 浏览器打开
     */
    fun openBrowser(context: Context, url: String?) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun isInstalled(context: Context, packageName: String?): Boolean {
        var packageInfo: PackageInfo?
        try {
            packageInfo = context.packageManager.getPackageInfo(packageName!!, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            packageInfo = null
            e.printStackTrace()
        }
        return true
    }
}