package com.dian.demo.utils.webview.command

import com.dian.demo.MainToWebInterface


/**
 * @author created by liyihuanx
 * @date 2021/11/10
 * @description: 类的描述
 */
interface Command {
    fun name(): String
    fun execute(
        parameters: Map<*, *>?,
        callback: MainToWebInterface? = null
    )
}