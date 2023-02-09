package com.dian.demo

import android.app.Application
import android.content.Context
import com.dian.demo.config.AppConfig.WB_APP_KEY
import com.dian.demo.config.AppConfig.WB_REDIRECT_URl
import com.dian.demo.config.AppConfig.WB_SCOPE
import com.dian.demo.http.HttpUtils
import com.dian.demo.utils.ActivityManager
import com.dian.demo.utils.ExceptionHandlerUtil
import com.sina.weibo.sdk.auth.AuthInfo
import com.sina.weibo.sdk.openapi.SdkListener
import com.sina.weibo.sdk.openapi.WBAPIFactory

/**
 * 1.视频播放器                    √
 * 2.地址选择器                    √
 * 3.日期选择器
 * 4.封装图片选择器                 √
 * 5.网络请求使用moshi代替gson      √
 * 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
 */
class ProjectApplication : Application() {
    companion object {

        private var mContext: Context? = null


        private var instance: Application? = null

        @JvmStatic
        fun getAppContext(): Context = mContext!!

        @JvmStatic
        fun getAppInstance(): Application = instance!!

    }

    override fun onCreate() {
        super.onCreate()
        init()
        initWeiBoSdk()
        HttpUtils.getInstance().init(this)

    }

  private fun initWeiBoSdk() {
        val authInfo = AuthInfo(this, WB_APP_KEY, WB_REDIRECT_URl, WB_SCOPE)
        WBAPIFactory.createWBAPI(this).registerApp(this, authInfo, object : SdkListener {
            override fun onInitSuccess() {
                // SDK初始化成功回调，成功一次后再次初始化将不再有任何回调
            }

            override fun onInitFailure(e: Exception) { // SDK初始化失败回调

            }
        })
    }

    private fun init() {
        mContext = this
        instance = this
        ExceptionHandlerUtil.init()
        ActivityManager.getInstance().init(this)
    }


}