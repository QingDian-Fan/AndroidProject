package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.aop.CheckNet

class MineListViewModel : BaseViewModel() {

    val articleData = MutableLiveData<List<ArticleBean>>()


    @CheckNet
    fun getMineShareData(page: Int) {
        launchOnUI {
            repo.getMineShareList(page)
                .onCompletion {
                    showLoadingView(false)
                }
                .onSuccess { it ->
                    it?.shareArticles?.datas.let {
                        articleData.value = it
                    }
                }
                .onFailure { code, message ->
                    LogUtil.e("TAG----->Request", "articleList失败：code：${code},message:${message}")
                    showErrorView(true)
                }
                .onCatch {
                    LogUtil.e(
                        "TAG----->Request",
                        "articleList失败：code：${it.errorCode},message:${it.errorMsg}"
                    )
                    showErrorView(true)
                }
        }

    }

    @CheckNet
    fun getMineCollectData(page: Int) {
        launchOnUI {
            repo.getMineCollectList(page)
                .onCompletion {
                    showLoadingView(false)
                }
                .onSuccess { it ->
                    it?.datas.let {
                        articleData.value = it
                    }
                }
                .onFailure { code, message ->
                    LogUtil.e("TAG----->Request", "articleList失败：code：${code},message:${message}")
                    showErrorView(true)
                }
                .onCatch {
                    LogUtil.e(
                        "TAG----->Request",
                        "articleList失败：code：${it.errorCode},message:${it.errorMsg}"
                    )
                    showErrorView(true)
                }
        }

    }
}