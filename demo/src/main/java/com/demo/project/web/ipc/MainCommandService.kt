package com.demo.project.web.ipc

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import com.common.utils.LogUtil
import com.common.weight.webview.command.CommandCallback
import com.common.weight.webview.dispatcher.InProcessCommandBridge

/** 主进程 Service：复用现有进程内命令实现，不改变命令注册和执行逻辑。 */
class MainCommandService : Service() {

    private val commandBridge = InProcessCommandBridge()

    private val binder = object : WebToMainInterface.Stub() {
        override fun handleWebCommand(
            commandName: String?,
            jsonParams: String?,
            callback: MainToWebInterface?
        ) {
            val safeCommandName = commandName?.takeIf { it.isNotEmpty() } ?: return
            commandBridge.dispatch(
                safeCommandName,
                jsonParams,
                CommandCallback { callbackName, response ->
                    try {
                        callback?.onResult(callbackName, response)
                    } catch (e: RemoteException) {
                        LogUtil.e(TAG, "return command result failed: ${e.message}")
                    }
                }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private companion object {
        const val TAG = "WebView-AIDL"
    }
}
