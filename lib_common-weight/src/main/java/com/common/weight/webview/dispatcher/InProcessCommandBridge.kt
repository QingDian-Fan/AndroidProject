package com.common.weight.webview.dispatcher

import com.common.utils.LogUtil
import com.common.weight.webview.command.CommandBridge
import com.common.weight.webview.command.CommandCallback
import com.google.gson.Gson

/**
 * 进程内默认实现：从 [WebCommandRegistry] 找到命令并直接执行。
 *
 * 多进程场景下，把这个实现替换成自己的 AIDL 转发实现即可，[WebCommandDispatcher] 不感知。
 */
class InProcessCommandBridge(
    private val gson: Gson = Gson()
) : CommandBridge {

    override fun dispatch(commandName: String, jsonParams: String?, callback: CommandCallback) {
        val command = WebCommandRegistry.get(commandName)
        if (command == null) {
            LogUtil.e("$TAG command not registered: $commandName")
            return
        }
        val params: Map<String, Any?>? = jsonParams
            ?.takeIf { it.isNotEmpty() }
            ?.let {
                runCatching {
                    @Suppress("UNCHECKED_CAST")
                    gson.fromJson(it, Map::class.java) as? Map<String, Any?>
                }.onFailure { e -> LogUtil.e("$TAG parse params failed: ${e.message}") }
                    .getOrNull()
            }
        command.execute(params, callback)
    }

    private companion object {
        const val TAG = "WebView"
    }
}
