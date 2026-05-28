package com.project.common.view.webview.utils

import com.project.common.utils.MoshiUtil
import com.project.common.utils.datastore.AppDataStore
import com.project.common.view.webview.bean.WebDataEntry
import com.project.common.view.webview.bean.isSame
import kotlin.collections.associateBy

object CollectWebPageUtil {
    private const val KEY_COLLECT_WEBPAGE = "KEY_COLLECT_WEBPAGE"

    fun getCollectWebPage(): MutableList<WebDataEntry> {
        val dataString = AppDataStore.getData(KEY_COLLECT_WEBPAGE, "")
        val dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)

        val resultList = dataList
            ?.associateBy { it.title to it.url }
            ?.values
            ?.sortedByDescending { it.timestamp }
        return resultList?.toMutableList() ?: mutableListOf()
    }

    fun collectWebPage(data: WebDataEntry) {
        val dataString = AppDataStore.getData(KEY_COLLECT_WEBPAGE, "")
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
        AppDataStore.putData(KEY_COLLECT_WEBPAGE, resultString)
    }

    fun removeCollectWebPage(data: WebDataEntry) {
        val dataString = AppDataStore.getData(KEY_COLLECT_WEBPAGE, "")
        var dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)
        if (dataList == null) {
            dataList = mutableListOf()
        }

        dataList.removeIf { it.isSame(data) }

        val resultString = MoshiUtil.toJsonList(dataList)
        AppDataStore.putData(KEY_COLLECT_WEBPAGE, resultString)
    }

    fun isCollectedWebPage(data: WebDataEntry): Boolean {
        val dataString = AppDataStore.getData(KEY_COLLECT_WEBPAGE, "")
        val dataList = MoshiUtil.fromJsonToList<WebDataEntry>(dataString)

        val resultList = dataList
            ?.associateBy { it.title to it.url }
            ?.values
            ?.sortedByDescending { it.timestamp }
        val exists = resultList?.any { it.isSame(data) }
        return exists ?: false

    }
}