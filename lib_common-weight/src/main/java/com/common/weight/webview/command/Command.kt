package com.common.weight.webview.command

/**
 * 原生侧可被 H5 触发的一个命令。命令通过 [name] 路由，业务参数从 [params] 取，
 * 需要回调时通过 [callback] 把 JSON 数据回灌给 H5。
 */
interface Command {

    /** H5 调用时 `JsParam.name` 必须与该值一致。 */
    fun name(): String

    /**
     * @param params   命令参数，由 H5 端传入的 JSON 反序列化得到（可能为 null）
     * @param callback 业务执行完毕后用来回灌结果；不需要回灌时为 null
     */
    fun execute(
        params: Map<String, Any?>?,
        callback: CommandCallback? = null
    )
}
