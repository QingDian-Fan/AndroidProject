package com.demo.project.web.command


import com.common.weight.webview.command.Command
import com.common.weight.webview.command.CommandCallback
import com.demo.project.ProjectApplication
import com.demo.project.ui.activity.CameraActivity





class CommandOpenActivity : Command {
    override fun name(): String {
        return "openActivity"
    }

    override fun execute(
        params: Map<String, Any?>?,
        callback: CommandCallback?
    ) {
        val path = when(params?.get("message")?.toString()) {
            "HomeActivity" -> {
                CameraActivity.start(ProjectApplication.getAppContext())
            }
            else -> {

            }
        }
    }




}