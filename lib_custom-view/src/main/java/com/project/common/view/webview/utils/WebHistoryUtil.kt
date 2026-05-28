package com.project.common.view.webview.utils


import com.project.common.utils.MoshiUtil
import com.project.common.utils.datastore.AppDataStore
import com.project.common.view.webview.bean.WebDataEntry
import kotlin.collections.associateBy

object WebHistoryUtil {
    private const val KEY_WEB_HISTORY = "KEY_WEB_HISTORY"

    fun getWebHistoryList(): MutableList<WebDataEntry> {
        val dataString = AppDataStore.getData(KEY_WEB_HISTORY, "")
        val dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)

        val resultList = dataList
            ?.associateBy { it.title to it.url }
            ?.values
            ?.sortedByDescending { it.timestamp }
        return resultList?.toMutableList() ?: mutableListOf()
    }

    fun putWebHistory(data: WebDataEntry) {
        val dataString = AppDataStore.getData(KEY_WEB_HISTORY, "")
        var dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)
        if (dataList == null) {
            dataList = mutableListOf()
        }
        dataList.add(data)
        val resultList = dataList
            .associateBy { it.title to it.url }
            .values
            .sortedByDescending { it.timestamp }
            .take(100)
            .toMutableList()
        val resultString = MoshiUtil.toJsonList(resultList)
        AppDataStore.putData(KEY_WEB_HISTORY, resultString)
    }
}