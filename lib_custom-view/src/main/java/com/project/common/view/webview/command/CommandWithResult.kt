package com.project.common.view.webview.command

import com.dian.demo.MainToWebInterface
import com.google.auto.service.AutoService
import com.google.gson.Gson
import java.util.*

/**
 * @author created by liyihuanx
 * @date 2021/11/10
 * @description: 类的描述
 */
@AutoService(Command::class)
class CommandWithResult : Command {

    override fun name(): String {
        return "login"
    }

    override fun execute(
        parameters: Map<*, *>?,
        callback: MainToWebInterface?
    ) {
        val map = HashMap<Any?, Any?>()
        map["accountName"] = "执行操作后的数据，给H5"
        callback?.onResult(parameters?.get("callbackname")?.toString(), Gson().toJson(map))
    }


}