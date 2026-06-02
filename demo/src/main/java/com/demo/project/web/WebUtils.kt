package com.demo.project.web

import com.common.weight.webview.dispatcher.WebCommandRegistry
import com.demo.project.web.command.CommandOpenActivity
import com.demo.project.web.command.CommandShowToast
import com.demo.project.web.command.CommandWithResult

object WebUtils {
    fun initCommand(){
        WebCommandRegistry.registerAll(listOf(
            CommandShowToast(),
            CommandOpenActivity(),
            CommandWithResult()
        ))
    }
}