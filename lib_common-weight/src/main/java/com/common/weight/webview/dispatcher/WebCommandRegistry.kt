package com.common.weight.webview.dispatcher

import com.common.weight.webview.command.Command
import java.util.concurrent.ConcurrentHashMap

/**
 * 命令注册中心：[com.common.weight.webview.dispatcher.InProcessCommandBridge] 会从这里
 * 查找 H5 调用对应的 [Command]。
 *
 * 宿主可在 `Application.onCreate` 中预先注册全部命令；也可以通过 [java.util.ServiceLoader]
 * 反射收集所有 `Command` 实现，按需 [register]。
 */
object WebCommandRegistry {

    private val commands = ConcurrentHashMap<String, Command>()

    fun register(command: Command) {
        commands.putIfAbsent(command.name(), command)
    }

    fun registerAll(commands: Iterable<Command>) {
        commands.forEach { register(it) }
    }

    fun unregister(name: String) {
        commands.remove(name)
    }

    fun get(name: String): Command? = commands[name]

    fun clear() {
        commands.clear()
    }
}
