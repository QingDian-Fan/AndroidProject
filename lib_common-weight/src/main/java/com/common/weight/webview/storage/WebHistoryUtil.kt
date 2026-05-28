package com.common.weight.webview.storage

import com.common.weight.webview.bean.WebDataEntry

/** 浏览历史：默认最多保留 100 条。 */
object WebHistoryUtil : WebPageStore(
    storageKey = "KEY_WEB_HISTORY",
    limit = 100
) {

    @JvmStatic
    fun getWebHistoryList(): MutableList<WebDataEntry> = getAll()

    @JvmStatic
    fun putWebHistory(data: WebDataEntry) = add(data)
}
