package com.dian.demo.utils

import com.dian.demo.di.model.SearchRecord
import com.dian.demo.utils.datastore.AppDataStore
import com.squareup.moshi.Types

object SearchRecordUtil {
    fun getLocalSearchList(): MutableList<SearchRecord>? {
        val dataString = AppDataStore.getData("LOCAL_SEARCH_HISTORY_RECORD", "")

        if (dataString.isNullOrEmpty()) return mutableListOf()

        // 创建 List<SearchRecord> 的类型
        val type = Types.newParameterizedType(List::class.java, SearchRecord::class.java)

        // 使用 Moshi 解析
        val dataList = MoshiUtil.fromJson<List<SearchRecord>>(dataString, type)

        return dataList?.toMutableList() ?: mutableListOf()
    }


    fun putLocalHistoryRecord(data:SearchRecord){
        val dataList: MutableList<SearchRecord> = getLocalSearchList()?:emptyList<SearchRecord>().toMutableList()
       val mDataList = dataList.filter{
           it.name!=data.name
       }.toMutableList()
        mDataList.add(data)
        val dataString = MoshiUtil.toJson(mDataList)
        AppDataStore.putData("LOCAL_SEARCH_HISTORY_RECORD",dataString)
    }
}