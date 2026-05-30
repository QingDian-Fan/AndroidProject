package com.common.utils.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.common.utils.permissions.PermissionConversionUtil.conversionPermission

object PermissionsUtil {

    fun hasPermission(context: Context, vararg permissions: String): Boolean =
        hasAllGranted(context, conversionPermission(permissions))

    fun hasPermission(context: Context, permissions: List<String>): Boolean =
        hasAllGranted(context, conversionPermission(permissions.toTypedArray()))

    fun hasAopPermission(context: Context, permissions: Array<out String>): Boolean =
        hasAllGranted(context, conversionPermission(permissions))

    /**
     * 校验前统一做版本适配（与申请时一致），避免高版本上校验仍针对失效的
     * READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE 而恒为 false。
     */
    private fun hasAllGranted(context: Context, permissions: Array<out String>): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }
}
