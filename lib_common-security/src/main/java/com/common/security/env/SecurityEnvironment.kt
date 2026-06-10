package com.common.security.env

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import java.io.File

object SecurityEnvironment {
    @JvmStatic
    fun collect(context: Context): SecurityCheckResult {
        return SecurityCheckResult(
            debuggable = isDebuggable(context),
            adbEnabled = isAdbEnabled(context),
            rooted = isRooted(),
            emulator = isEmulator()
        )
    }

    @JvmStatic
    fun isDebuggable(context: Context): Boolean {
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    @JvmStatic
    fun isAdbEnabled(context: Context): Boolean {
        return runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        }.getOrDefault(false)
    }

    @JvmStatic
    fun isRooted(): Boolean {
        return hasTestKeys() || hasSuFile() || canFindSuCommand()
    }

    @JvmStatic
    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.orEmpty().lowercase()
        val model = Build.MODEL.orEmpty().lowercase()
        val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
        val brand = Build.BRAND.orEmpty().lowercase()
        val device = Build.DEVICE.orEmpty().lowercase()
        val product = Build.PRODUCT.orEmpty().lowercase()
        val hardware = Build.HARDWARE.orEmpty().lowercase()

        return fingerprint.startsWith("generic") ||
            fingerprint.contains("vbox") ||
            fingerprint.contains("test-keys") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            manufacturer.contains("genymotion") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            brand.startsWith("generic") && device.startsWith("generic")
    }

    private fun hasTestKeys(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    private fun hasSuFile(): Boolean {
        return SU_PATHS.any { File(it).exists() }
    }

    private fun canFindSuCommand(): Boolean {
        var process: Process? = null
        return runCatching {
            process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            process?.inputStream?.bufferedReader()?.use { it.readLine() }?.isNotBlank() == true
        }.getOrDefault(false).also {
            process?.destroy()
        }
    }

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/system/su",
        "/system/bin/.ext/su",
        "/system/usr/we-need-root/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/data/local/su"
    )
}
