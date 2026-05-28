package com.common.weight.webview.command

/**
 * 原生命令执行完毕后，把结果回调给 H5。
 *
 * 这是对原 AIDL `MainToWebInterface` 的纯 Kotlin 抽象。默认实现走进程内 (in-process)
 * 直接调用；如果业务需要 WebView 单独进程，可以让宿主再实现一层 AIDL 适配，把这里的
 * 调用桥接到主进程。
 */
fun interface CommandCallback {
    /**
     * @param callbackName H5 通过参数传入的回调名（一般是 `demojs.callback` 的第一参数）
     * @param response     业务返回数据，约定为合法 JSON 字符串
     */
    fun onResult(callbackName: String, response: String?)
}
