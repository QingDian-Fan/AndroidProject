package com.dian.demo.utils.webview.command


import com.dian.demo.MainToWebInterface
import com.dian.demo.ProjectApplication
import com.dian.demo.ui.activity.HomeActivity
import com.google.auto.service.AutoService



@AutoService(Command::class)
class CommandOpenActivity : Command {
    override fun name(): String {
        return "openActivity"
    }

    override fun execute(
        parameters: Map<*, *>?,
        callback: MainToWebInterface?
    ) {
        val path = when(parameters?.get("message")?.toString()) {
            "HomeActivity" -> {
                HomeActivity.start(ProjectApplication.getAppContext())
            }
            else -> {

            }
        }
    }


}