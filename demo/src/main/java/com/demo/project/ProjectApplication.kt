package com.demo.project

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.webkit.WebView
import com.common.auth.AuthManager
import com.common.auth.hook.LoginHookUtil
import com.common.http.HttpUtils
import com.common.scan.wechat.WeChatQRCodeDetector
import com.common.share.WeiBoSdkUtils
import com.common.theme.BaseApplication
import com.common.utils.ExceptionHandlerUtil
import com.common.utils.LogFileUtil
import com.common.utils.Utils
import com.common.weight.webview.dispatcher.WebCommandRegistry
import com.common.weight.webview.dispatcher.WebCommandDispatcher
import com.demo.project.web.WebUtils
import com.demo.project.web.command.CommandOpenActivity
import com.demo.project.web.command.CommandShowToast
import com.demo.project.web.command.CommandWithResult
import com.demo.project.web.ipc.AidlCommandBridge
import org.opencv.OpenCV

class ProjectApplication: BaseApplication() {

    companion object {

        private const val WEB_PROCESS_SUFFIX = "web"

        private var mContext: Context? = null


        private var instance: BaseApplication? = null

        @JvmStatic
        fun getAppContext(): Context = checkNotNull(mContext) {
            "ProjectApplication context is not initialized."
        }

        @JvmStatic
        fun getAppInstance(): BaseApplication = checkNotNull(instance) {
            "ProjectApplication instance is not initialized."
        }

    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isWebProcess(base)) {
            WebView.setDataDirectorySuffix(WEB_PROCESS_SUFFIX)
        }
    }

    override fun onCreate() {
        super.onCreate()
        init()
        //初始化Utils库
        Utils.init(this,BuildConfig.isDebug)
        //初始化网络请求
        HttpUtils.getInstance().init()
        //初始化opencv 二维码扫描
        OpenCV.initOpenCV()
        WeChatQRCodeDetector.init(this)
        //初始化wjs交互
        WebUtils.initCommand()
        if (isWebProcess(this)) {
            WebCommandDispatcher.setBridge(AidlCommandBridge(this))
        }
        //初始化登陆页面
        AuthManager.init()
        LoginHookUtil.HookAms(this)
        //初始化日志库
        ExceptionHandlerUtil.init(this)
        LogFileUtil.init(this)
        WeiBoSdkUtils.initWeiBoSdk(this)
    }

    private fun init() {
        mContext = this
        instance = this
    }

    private fun isWebProcess(context: Context): Boolean {
        return currentProcessName(context) == "${context.packageName}:$WEB_PROCESS_SUFFIX"
    }

    private fun currentProcessName(context: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return Application.getProcessName()
        }
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val pid = Process.myPid()
        return activityManager?.runningAppProcesses
            ?.firstOrNull { it.pid == pid }
            ?.processName
    }

}
