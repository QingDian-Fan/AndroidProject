package com.project.common.utils.permissions

import android.Manifest
import android.os.Build

object PermissionConversionUtil {

    fun conversionPermission(permissions: Array<out String>): Array<out String> {
        val mPermissions = permissions.toMutableList()
        if (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)
            || permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            || permissions.contains(Manifest.permission.READ_MEDIA_IMAGES)
            || permissions.contains(Manifest.permission.READ_MEDIA_VIDEO)
            || permissions.contains(Manifest.permission.READ_MEDIA_AUDIO)
        ) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                mPermissions.remove(Manifest.permission.READ_EXTERNAL_STORAGE)
                mPermissions.remove(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                mPermissions.remove(Manifest.permission.READ_MEDIA_IMAGES)
                mPermissions.remove(Manifest.permission.READ_MEDIA_VIDEO)
                mPermissions.remove(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
        return mPermissions.toTypedArray()
    }
}