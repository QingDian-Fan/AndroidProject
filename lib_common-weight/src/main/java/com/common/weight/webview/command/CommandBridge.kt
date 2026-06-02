package com.common.weight.webview.command

/**
 * 把 H5 命令转交给真正的执行方。原项目里这步走的是 AIDL，跨进程调用主进程的命令集合；
 * 在通用库里抽象成接口，宿主可以：
 *
 *  - 默认：使用 [com.common.weight.webview.dispatcher.InProcessCommandBridge] 直接进程内调用；
 *  - 多进程：自行实现该接口，把 [dispatch] 转发到 AIDL Service。
 */
interface CommandBridge {
    fun dispatch(commandName: String, jsonParams: String?, callback: CommandCallback)
}
