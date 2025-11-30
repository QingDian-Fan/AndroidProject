package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.SearchRecord
import com.dian.demo.utils.SearchRecordUtil
import com.dian.demo.utils.datastore.AppDataStore
import kotlin.collections.toMutableList

class SearchViewModel: BaseViewModel() {

    val mHotSearchList by lazy {
        MutableLiveData<MutableList<SearchRecord>>()
    }

    val mLocalSearchList by lazy {
        MutableLiveData<MutableList<SearchRecord>>()
    }

    val clickData by lazy {
        MutableLiveData<SearchRecord>()
    }
    val loadData by lazy {
        MutableLiveData<SearchData>()
    }

    val articleData = MutableLiveData<List<ArticleBean>>()


    fun getSearchHotHistoryRecord(){
        launchOnUI {
            repo.getSearchHotHistoryRecord()
                .onCompletion {
                    showLoadingView(false)

                }
                .onSuccess {
                    mHotSearchList.value = it?.toMutableList()
                }
                .onFailure { _, _ ->
                    showErrorView(true)
                }
                .onCatch {
                    showErrorView(true)
                }
        }
    }

    fun getHistoryRecord(){
        launchOnUI {
            val localList =  SearchRecordUtil.getLocalSearchList()
            localList?.let {
                mLocalSearchList.value = localList
            }
        }
    }

    fun putHistoryRecord(data: SearchRecord){
        launchOnUI {
            val localList = mLocalSearchList.value?:emptyList<SearchRecord>().toMutableList()
            localList.add(data)
            mLocalSearchList.value = localList
            SearchRecordUtil.putLocalHistoryRecord(data)
        }
    }

    fun getSearchList(mPage:Int=0,keyword: String){
        launchOnUI {
            repo.getSearchList(mPage,keyword)
                .onCompletion {
                    showLoadingView(false)

                }
                .onSuccess {
                    it?.datas?.let {
                        articleData.value = it
                    }
                }
                .onFailure { _, _ ->
                    showErrorView(true)
                }
                .onCatch {
                    showErrorView(true)
                }
        }
    }
}