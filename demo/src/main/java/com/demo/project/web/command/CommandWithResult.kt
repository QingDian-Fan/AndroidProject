package com.demo.project.web.command


import com.common.weight.webview.command.Command
import com.common.weight.webview.command.CommandCallback
import java.util.*

/**
 * @author created by liyihuanx
 * @date 2021/11/10
 * @description: 类的描述
 */

class CommandWithResult : Command {

    override fun name(): String {
        return "login"
    }

    override fun execute(
        params: Map<String, Any?>?,
        callback: CommandCallback?
    ) {
        val map = HashMap<Any?, Any?>()
        map["accountName"] = "执行操作后的数据，给H5"
        callback?.onResult(params?.get("callbackname")?.toString()!!, "{\"accountName\":\"执行操作后的数据，给H5\"}")
    }




}