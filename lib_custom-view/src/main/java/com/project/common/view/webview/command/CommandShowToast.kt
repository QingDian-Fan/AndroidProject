package com.project.common.view.webview.command

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.dian.demo.MainToWebInterface
import com.dian.demo.ProjectApplication
import com.google.auto.service.AutoService

@AutoService(Command::class)
class CommandShowToast : Command {
    override fun name(): String {
        return "showToast"
    }

    override fun execute(
        parameters: Map<*, *>?,
        callback: MainToWebInterface?
    ) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            Toast.makeText(ProjectApplication.getAppContext(), parameters?.get("message")?.toString(), Toast.LENGTH_SHORT).show()
        }
    }


}