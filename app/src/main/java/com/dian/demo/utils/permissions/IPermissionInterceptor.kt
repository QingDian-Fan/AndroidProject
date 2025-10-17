package com.dian.demo.utils.permissions

import android.app.Activity
import androidx.fragment.app.FragmentActivity


interface IPermissionInterceptor {
    fun launchPermissionRequest(activity: FragmentActivity, allPermissions: Array<out String>)

    fun finishPermissionRequest(activity: FragmentActivity, allPermissions: Array<out String>)
}