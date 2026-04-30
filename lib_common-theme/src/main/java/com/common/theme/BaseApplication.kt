package com.common.theme

import android.app.Application
import android.content.Context



open class BaseApplication : Application() {
    companion object {

        private var mContext: Context? = null


        private var instance: BaseApplication? = null

        @JvmStatic
        fun getAppContext(): Context = mContext!!

        @JvmStatic
        fun getAppInstance(): BaseApplication = instance!!

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