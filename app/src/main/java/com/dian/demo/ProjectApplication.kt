package com.dian.demo

import android.app.Application
import android.content.Context


class ProjectApplication : Application() {
    companion object {
        @Volatile
        private var mContext: Context? = null

        @Volatile
        private var instance: Application? = null

        @JvmStatic
        fun getAppContext(): Context = mContext!!

        @JvmStatic
        fun getAppInstance(): Application = instance!!

    }

    override fun onCreate() {
        super.onCreate()
        init()
       // initWeiBoSdk()
        //HttpUtils.getInstance().init(this)
    }

  /*  private fun initWeiBoSdk() {
        val authInfo = AuthInfo(this, WB_APP_KEY, WB_REDIRECT_URl, WB_SCOPE)
        WBAPIFactory.createWBAPI(this).registerApp(this, authInfo, object : SdkListener {
            override fun onInitSuccess() {
                // SDK初始化成功回调，成功一次后再次初始化将不再有任何回调
            }

            override fun onInitFailure(e: Exception) { // SDK初始化失败回调

            }
        })
    }*/

    private fun init() {
        mContext = this
        instance = this
        //ExceptionHandlerUtil.init()
       // NightModeUtil.initNightMode()
    }


}