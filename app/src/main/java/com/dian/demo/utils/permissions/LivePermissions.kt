package com.dian.demo.utils.permissions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData

class LivePermissions {

    companion object {
        const val TAG = "permissions"

        fun getInstance(activity: AppCompatActivity): LivePermissions {
            return LivePermissions(activity)
        }

        fun getInstance(fragment: Fragment): LivePermissions {
            return LivePermissions(fragment)
        }
    }

    constructor(activity: AppCompatActivity) {
        liveFragment = getInstance(activity.supportFragmentManager)
    }

    constructor(fragment: Fragment) {
        liveFragment = getInstance(fragment.childFragmentManager)
    }

    @Volatile
    private var liveFragment: LiveFragment? = null

    private fun getInstance(fragmentManager: FragmentManager) =
        liveFragment ?: synchronized(this) {
            val existingFragment = fragmentManager.findFragmentByTag(TAG)
            liveFragment ?: if (existingFragment is LiveFragment) {
                existingFragment
            } else LiveFragment().run {
                fragmentManager.beginTransaction().add(this, TAG).commitNow()
                this
            }
        }

    private var interceptor: IPermissionInterceptor? = null

    fun addInterceptor(interceptor: IPermissionInterceptor): LivePermissions {
        this.interceptor = interceptor
        return this
    }

    fun request(vararg permissions: String): MutableLiveData<PermissionResult> {
        return this.requestArray(permissions)
    }

    fun requestArray(permissions: Array<out String>): MutableLiveData<PermissionResult> {
        val fragment = liveFragment ?: return MutableLiveData<PermissionResult>().apply {
            value = PermissionResult.Deny(permissions.toList().toTypedArray())
        }
        interceptor?.apply {
            fragment.addInterceptor(interceptor)
        }
        fragment.requestPermissions(permissions)
        return fragment.liveData
    }

}
