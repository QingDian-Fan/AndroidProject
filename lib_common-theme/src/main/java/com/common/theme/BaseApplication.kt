package com.common.theme

import android.app.Application
import android.content.Context



open class BaseApplication : Application() {
    companion object {

        private var mContext: Context? = null


        private var instance: BaseApplication? = null

        @JvmStatic
        fun getAppContext(): Context = checkNotNull(mContext) {
            "BaseApplication context is not initialized."
        }

        @JvmStatic
        fun getAppInstance(): BaseApplication = checkNotNull(instance) {
            "BaseApplication instance is not initialized."
        }

    }

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        mContext = this
        instance = this
    }
}
