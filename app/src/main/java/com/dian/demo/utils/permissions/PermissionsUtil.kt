package com.dian.demo.utils.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionsUtil {
    fun hasPermission(context: Context, vararg  permissions: String): Boolean {
        for (permission in permissions) {
            val granted = ContextCompat.checkSelfPermission(context, permission)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun hasPermission(context: Context, permissions:List<String>): Boolean {
        for (permission in permissions) {
            val granted = ContextCompat.checkSelfPermission(context, permission)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun hasAopPermission(context: Context, permissions: Array<out String>): Boolean {
        for (permission in permissions) {
            val granted = ContextCompat.checkSelfPermission(context, permission)
            if (granted != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}