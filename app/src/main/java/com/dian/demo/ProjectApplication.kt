package com.dian.demo

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.dian.demo.BuildConfig
import com.dian.demo.config.AppConfig.WB_APP_KEY
import com.dian.demo.config.AppConfig.WB_REDIRECT_URl
import com.dian.demo.config.AppConfig.WB_SCOPE
import com.dian.demo.http.HttpUtils
import com.dian.demo.skin.SkinApplication
import com.dian.demo.ui.status.Gloading.Companion.debug
import com.dian.demo.ui.status.Gloading.Companion.initDefault
import com.dian.demo.ui.status.GlobalAdapter
import com.dian.demo.utils.ActivityManager
import com.dian.demo.utils.ExceptionHandlerUtil
import com.dian.demo.utils.LogFileUtil
import com.dian.demo.utils.Utils
import com.dian.demo.utils.datastore.AppDataStore
import com.dian.demo.utils.gray.GlobalGray
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.sina.weibo.sdk.auth.AuthInfo
import com.sina.weibo.sdk.openapi.SdkListener
import com.sina.weibo.sdk.openapi.WBAPIFactory
import com.tencent.bugly.crashreport.CrashReport


class ProjectApplication : SkinApplication() {
    companion object {

        private var mContext: Context? = null


        private var instance: ProjectApplication? = null

        @JvmStatic
        fun getAppContext(): Context = mContext!!

        @JvmStatic
        fun getAppInstance(): ProjectApplication = instance!!



    }
    init {
        debug(BuildConfig.DEBUG)
        initDefault(GlobalAdapter())
    }

    override fun onCreate() {
        super.onCreate()
        init()
        initWeiBoSdk()
        HttpUtils.getInstance().init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (isOtherProcess()) {
                WebView.setDataDirectorySuffix(getProcessNameSuffix())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun isOtherProcess(): Boolean {
        val process = getProcessName()
        return process != packageName
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getProcessNameSuffix(): String {
        val full = getProcessName()
        return full.substringAfterLast(":", "")
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
        if (AppDataStore.getData("isGray", false)) {
            GlobalGray.hook()
        }


        ExceptionHandlerUtil.init(this)
        LogFileUtil.init(this)
        ActivityManager.getInstance().init(this)
       // LifecycleLogger().install(this)

        CrashReport.initCrashReport(applicationContext, "2e9e288d60", true)
        CrashReport.setAppChannel(applicationContext, Utils.getChannel())
        CrashReport.setAppVersion(applicationContext, BuildConfig.VERSION_NAME)
        CrashReport.setAppPackage(applicationContext, packageName)


        //设置全局的Header构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
            ClassicsHeader(context)
        }
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout -> //指定为经典Footer，默认是 BallPulseFooter
            ClassicsFooter(context).setDrawableSize(20f)
        }
    }






}