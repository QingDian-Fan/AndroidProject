package com.common.weight.webview.storage

import com.common.weight.webview.bean.WebDataEntry

/** 收藏：不做条数限制。 */
object CollectWebPageUtil : WebPageStore(storageKey = "KEY_COLLECT_WEBPAGE") {

    @JvmStatic
    fun getCollectWebPage(): MutableList<WebDataEntry> = getAll()

    @JvmStatic
    fun collectWebPage(data: WebDataEntry) = add(data)

    @JvmStatic
    fun removeCollectWebPage(data: WebDataEntry) = remove(data)

    @JvmStatic
    fun isCollectedWebPage(data: WebDataEntry): Boolean = contains(data)
}
