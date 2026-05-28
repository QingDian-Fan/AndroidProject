package com.common.weight.webview.dispatcher

import com.common.utils.LogUtil
import com.common.weight.webview.command.CommandBridge
import com.common.weight.webview.command.CommandCallback
import com.common.weight.webview.webview.BaseWebView

/**
 * H5 → 原生命令分发器。
 *
 *  - 默认走 [InProcessCommandBridge]，宿主只需要在 `Application` 启动时通过
 *    [WebCommandRegistry.register] 注册 [com.common.weight.webview.command.Command] 即可。
 *  - 如果业务需要多进程隔离，调用 [setBridge] 注入自定义的 [CommandBridge]（例如 AIDL 适配）。
 */
object WebCommandDispatcher {

    @Volatile
    private var bridge: CommandBridge = InProcessCommandBridge()

    fun setBridge(bridge: CommandBridge) {
        this.bridge = bridge
    }

    fun executeCommand(commandName: String, jsonParams: String?, webView: BaseWebView) {
        LogUtil.e(TAG, "executeCommand: $commandName params=$jsonParams")
        try {
            bridge.dispatch(
                commandName,
                jsonParams,
                CommandCallback { name, response -> webView.handleCallback(name, response) }
            )
        } catch (e: Exception) {
            LogUtil.e(TAG, "dispatch failed: ${e.message}")
        }
    }

    private const val TAG = "WebView"
}
