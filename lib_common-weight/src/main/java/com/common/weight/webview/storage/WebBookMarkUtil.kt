package com.common.weight.webview.storage

import com.common.weight.webview.bean.WebDataEntry

/** 书签：不做条数限制。 */
object WebBookMarkUtil : WebPageStore(storageKey = "KEY_WEB_BOOK_MARK") {

    @JvmStatic
    fun getMarkWebPage(): MutableList<WebDataEntry> = getAll()

    @JvmStatic
    fun markWebPage(data: WebDataEntry) = add(data)

    @JvmStatic
    fun removeMarkWebPage(data: WebDataEntry) = remove(data)

    @JvmStatic
    fun isMarkWebPage(data: WebDataEntry): Boolean = contains(data)
}
