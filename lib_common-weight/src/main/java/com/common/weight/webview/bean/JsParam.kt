package com.common.weight.webview.bean

import com.google.gson.JsonObject

/**
 * H5 通过 `window.webview.takeNativeAction(jsonString)` 投递给原生侧的统一参数结构。
 *
 *  - [name]  对应 [com.common.weight.webview.command.Command.name]，原生用它路由到具体命令。
 *  - [param] 命令携带的业务参数，原生再次解析为业务模型。
 */
data class JsParam(
    val name: String,
    val param: JsonObject
)
