package com.dian.demo.utils.webview

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.dian.demo.MainToWebInterface
import com.dian.demo.ProjectApplication
import com.dian.demo.WebToMainInterface
import com.dian.demo.utils.webview.mainprocess.MainCommandService
import com.dian.demo.utils.webview.webview.BaseWebView

/**
 * @author created by liyihuanx
 * @date 2021/11/10
 * @description: 类的描述
 */
class WebCommandDispatcher private constructor() : ServiceConnection {
    private var iWebviewProcessToMainProcessInterface: WebToMainInterface? = null

    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            WebCommandDispatcher()
        }
    }

    fun initAidlConnection() {
        val intent = Intent(ProjectApplication.getAppContext(), MainCommandService::class.java)
        ProjectApplication.getAppContext().bindService(intent, this, Context.BIND_AUTO_CREATE)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        iWebviewProcessToMainProcessInterface =
            WebToMainInterface.Stub.asInterface(service)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        iWebviewProcessToMainProcessInterface = null
        initAidlConnection()
    }

    override fun onBindingDied(name: ComponentName?) {
        iWebviewProcessToMainProcessInterface = null
        initAidlConnection()
    }

    fun executeCommand(commandName: String, params: String?, baseWebView: BaseWebView) {
        try {
            iWebviewProcessToMainProcessInterface?.handleWebCommand(
                commandName,
                params,
                object : MainToWebInterface.Stub() {
                    override fun onResult(callbackname: String, response: String?) {
                        baseWebView.handleCallback(callbackname, response)
                    }
                })
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }
}