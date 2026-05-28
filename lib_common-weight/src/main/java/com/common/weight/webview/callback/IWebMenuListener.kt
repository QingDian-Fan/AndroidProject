package com.common.weight.webview.callback

/** 浏览器顶部/底部菜单常用动作回调。 */
interface IWebMenuListener {
    fun onHome()
    fun onTop()
    fun onRefresh()
    fun onClose()
    fun onCollect()
    fun onMark()
    fun onShare()
    fun onSetting()
}
