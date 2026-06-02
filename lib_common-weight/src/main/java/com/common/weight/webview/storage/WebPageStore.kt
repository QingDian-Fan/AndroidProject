package com.common.weight.webview.storage

import com.common.utils.datastore.AppDataStore
import com.common.utils.moshi.MoshiUtil
import com.common.weight.webview.bean.WebDataEntry
import com.common.weight.webview.bean.isSame

/**
 * 浏览历史、书签、收藏共用的存储模板：
 *
 *  1. 全部记录以 JSON 数组形式存到 [AppDataStore]；
 *  2. 读/写都按 `(title, url)` 去重；
 *  3. 按 `timestamp` 倒序；
 *  4. 写入时支持 [limit] 截断，避免持续膨胀。
 *
 * 调用方只需提供存储 key 与可选的最大保留条数。
 */
open class WebPageStore(
    private val storageKey: String,
    private val limit: Int = Int.MAX_VALUE
) {

    fun getAll(): MutableList<WebDataEntry> = readSorted().toMutableList()

    fun add(data: WebDataEntry) {
        val current = readRaw().toMutableList().apply { add(data) }
        val deduped = dedupAndSort(current).let {
            if (it.size > limit) it.take(limit) else it
        }
        writeRaw(deduped)
    }

    fun remove(data: WebDataEntry) {
        val current = readRaw().toMutableList()
        val changed = current.removeAll { it.isSame(data) }
        if (changed) writeRaw(current)
    }

    fun contains(data: WebDataEntry): Boolean = readSorted().any { it.isSame(data) }

    fun clear() {
        writeRaw(emptyList())
    }

    private fun readRaw(): List<WebDataEntry> {
        val json = AppDataStore.getData(storageKey, "")
        if (json.isEmpty()) return emptyList()
        return MoshiUtil.fromJsonToList<WebDataEntry>(json).orEmpty()
    }

    private fun readSorted(): List<WebDataEntry> = dedupAndSort(readRaw())

    private fun writeRaw(list: List<WebDataEntry>) {
        AppDataStore.putData(storageKey, MoshiUtil.toJsonList(list))
    }

    private fun dedupAndSort(list: List<WebDataEntry>): List<WebDataEntry> =
        list.associateBy { it.title to it.url }
            .values
            .sortedByDescending { it.timestamp }
}
