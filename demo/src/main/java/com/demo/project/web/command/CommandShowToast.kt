package com.demo.project.web.command

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.common.weight.webview.command.Command
import com.common.weight.webview.command.CommandCallback
import com.demo.project.ProjectApplication


class CommandShowToast : Command {
    override fun name(): String {
        return "showToast"
    }

    override fun execute(
        params: Map<String, Any?>?,
        callback: CommandCallback?
    ) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(ProjectApplication.getAppContext(), params?.get("message")?.toString(), Toast.LENGTH_SHORT).show()
        }
    }




}