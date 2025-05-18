package com.dian.demo.utils.webview.command

import com.dian.demo.MainToWebInterface



interface Command {
    fun name(): String
    fun execute(
        parameters: Map<*, *>?,
        callback: MainToWebInterface? = null
    )
}