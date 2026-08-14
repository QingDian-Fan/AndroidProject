package com.demo.project.web.ipc

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import com.common.utils.LogUtil
import com.common.weight.webview.command.CommandBridge
import com.common.weight.webview.command.CommandCallback
import java.util.ArrayDeque

/**
 * WebView 进程使用的 [CommandBridge]：将原有 H5 命令原样转发给主进程执行。
 *
 * Service 连接建立前到达的少量命令会暂存，避免页面刚打开时的首个命令被静默丢弃。
 * Bridge 由 Application 持有，连接生命周期与 WebView 进程一致。
 */
class AidlCommandBridge(context: Context) : CommandBridge, ServiceConnection {

    private val appContext = context.applicationContext
    private val lock = Any()
    private val pendingCommands = ArrayDeque<PendingCommand>()

    @Volatile
    private var remote: WebToMainInterface? = null

    @Volatile
    private var isBound = false

    init {
        bindMainService()
    }

    override fun dispatch(
        commandName: String,
        jsonParams: String?,
        callback: CommandCallback
    ) {
        val command = PendingCommand(commandName, jsonParams, callback)
        val connectedRemote = remote
        if (connectedRemote != null) {
            sendCommand(connectedRemote, command)
            return
        }

        val reconnectedRemote = synchronized(lock) {
            remote ?: run {
                if (pendingCommands.size >= MAX_PENDING_COMMANDS) {
                    pendingCommands.removeFirst()
                    LogUtil.e(TAG, "pending command queue is full, discard oldest command")
                }
                pendingCommands.addLast(command)
                null
            }
        }
        if (reconnectedRemote != null) {
            sendCommand(reconnectedRemote, command)
        } else {
            bindMainService()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val connectedRemote = WebToMainInterface.Stub.asInterface(service)
        if (connectedRemote == null) {
            resetConnection(rebind = true)
            return
        }

        val commands = synchronized(lock) {
            remote = connectedRemote
            buildList {
                while (pendingCommands.isNotEmpty()) {
                    add(pendingCommands.removeFirst())
                }
            }
        }
        commands.forEach { sendCommand(connectedRemote, it) }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        remote = null
        // 系统会保留原绑定并在 Service 恢复后再次回调 onServiceConnected。
    }

    override fun onBindingDied(name: ComponentName?) {
        resetConnection(rebind = true)
    }

    override fun onNullBinding(name: ComponentName?) {
        LogUtil.e(TAG, "main command service returned a null binder")
        resetConnection(rebind = false)
        clearPendingCommands()
    }

    private fun bindMainService() {
        synchronized(lock) {
            if (isBound) return
            isBound = true
        }

        val didBind = runCatching {
            appContext.bindService(
                Intent(appContext, MainCommandService::class.java),
                this,
                Context.BIND_AUTO_CREATE
            )
        }.onFailure { LogUtil.e(TAG, "bind main command service failed: ${it.message}") }
            .getOrDefault(false)

        if (!didBind) {
            synchronized(lock) { isBound = false }
            clearPendingCommands()
            LogUtil.e(TAG, "main command service was not found")
        }
    }

    private fun sendCommand(
        connectedRemote: WebToMainInterface,
        command: PendingCommand
    ) {
        try {
            connectedRemote.handleWebCommand(
                command.name,
                command.jsonParams,
                object : MainToWebInterface.Stub() {
                    override fun onResult(callbackName: String?, response: String?) {
                        command.callback.onResult(callbackName.orEmpty(), response)
                    }
                }
            )
        } catch (e: RemoteException) {
            LogUtil.e(TAG, "dispatch command failed: ${e.message}")
            synchronized(lock) {
                if (pendingCommands.size < MAX_PENDING_COMMANDS) {
                    pendingCommands.addFirst(command)
                }
            }
            resetConnection(rebind = true)
        }
    }

    private fun resetConnection(rebind: Boolean) {
        val shouldUnbind = synchronized(lock) {
            remote = null
            val bound = isBound
            isBound = false
            bound
        }
        if (shouldUnbind) {
            runCatching { appContext.unbindService(this) }
        }
        if (rebind) bindMainService()
    }

    private fun clearPendingCommands() {
        synchronized(lock) { pendingCommands.clear() }
    }

    private data class PendingCommand(
        val name: String,
        val jsonParams: String?,
        val callback: CommandCallback
    )

    private companion object {
        const val TAG = "WebView-AIDL"
        const val MAX_PENDING_COMMANDS = 32
    }
}
