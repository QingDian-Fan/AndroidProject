package com.dian.demo.utils.webview.utils

import com.dian.demo.utils.MoshiUtil
import com.dian.demo.utils.datastore.AppDataStore
import com.dian.demo.utils.webview.bean.WebDataEntry
import com.dian.demo.utils.webview.bean.isSame
import kotlin.collections.associateBy

object WebBookMarkUtil {
    private const val KEY_WEB_BOOK_MARK = "KEY_WEB_BOOK_MARK"

    fun getMarkWebPage(): MutableList<WebDataEntry> {
        val dataString = AppDataStore.getData(KEY_WEB_BOOK_MARK, "")
        val dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)

        val resultList = dataList
            ?.associateBy { it.title to it.url }
            ?.values
            ?.sortedByDescending { it.timestamp }
        return resultList?.toMutableList() ?: mutableListOf()
    }

    fun markWebPage(data: WebDataEntry) {
        val dataString = AppDataStore.getData(KEY_WEB_BOOK_MARK, "")
        var dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)
        if (dataList == null) {
            dataList = mutableListOf()
        }
        dataList.add(data)
        val resultList = dataList
            .associateBy { it.title to it.url }
            .values
            .sortedByDescending { it.timestamp }
            .toMutableList()
        val resultString = MoshiUtil.toJsonList(resultList)
        AppDataStore.putData(KEY_WEB_BOOK_MARK, resultString)
    }

    fun removeMarkWebPage(data: WebDataEntry) {
        val dataString = AppDataStore.getData(KEY_WEB_BOOK_MARK, "")
        var dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)
        if (dataList == null) {
            dataList = mutableListOf()
        }

        dataList.removeIf { it.isSame(data) }

        val resultString = MoshiUtil.toJsonList(dataList)
        AppDataStore.putData(KEY_WEB_BOOK_MARK, resultString)
    }

    fun isMarkWebPage(data: WebDataEntry): Boolean {
        val dataString = AppDataStore.getData(KEY_WEB_BOOK_MARK, "")
        val dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)

        val resultList = dataList
            ?.associateBy { it.title to it.url }
            ?.values
            ?.sortedByDescending { it.timestamp }
        val exists = resultList?.any { it.isSame(data) }
        return exists ?: false

    }
}