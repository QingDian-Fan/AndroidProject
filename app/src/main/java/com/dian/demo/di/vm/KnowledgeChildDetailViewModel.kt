package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.ArticleBean

class KnowledgeChildDetailViewModel: BaseViewModel() {

    val articleData = MutableLiveData<List<ArticleBean>>()


    fun getKnowledgeDetailData(mPage:Int,cid: String){
        launchOnUI {
            repo.getKnowledgeDetailData(mPage,cid)
                .onCompletion {
                    showLoadingView(false)

                }
                .onSuccess {
                    if (it==null|| it.datas ==null|| it.datas.isEmpty()){
                        showEmptyView(true)
                        return@onSuccess
                    }
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