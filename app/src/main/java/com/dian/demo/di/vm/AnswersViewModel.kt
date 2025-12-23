package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.aop.CheckNet

class AnswersViewModel: BaseViewModel() {
    val articleData = MutableLiveData<List<ArticleBean>>()


    @CheckNet
    fun getAnswersList(page: Int) {
        launchOnUI {
            repo.getAnswersList(page)
                .onCompletion {
                    LogUtil.e("TAG----->Request", "articleList请求结束")
                    showLoadingView(false)
                }
                .onSuccess { it ->
                    it?.datas?.let {
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
    fun collectArticle(id: String){
        launchOnUI {
            repo.collectArticle(id)
                .onCompletion {
                    LogUtil.e("TAG----->Request", "articleList请求结束")
                }
                .onSuccess { it ->

                }
                .onFailure { code, message ->
                    LogUtil.e("TAG----->Request", "articleList失败：code：${code},message:${message}")
                }
                .onCatch {
                    LogUtil.e(
                        "TAG----->Request",
                        "articleList失败：code：${it.errorCode},message:${it.errorMsg}"
                    )
                }
        }
    }

    @CheckNet
    fun cancelCollectArticle(id: String){
        launchOnUI {
            repo.cancelCollectArticle(id)
                .onCompletion {
                    LogUtil.e("TAG----->Request", "articleList请求结束")
                }
                .onSuccess { it ->

                }
                .onFailure { code, message ->
                    LogUtil.e("TAG----->Request", "articleList失败：code：${code},message:${message}")
                }
                .onCatch {
                    LogUtil.e(
                        "TAG----->Request",
                        "articleList失败：code：${it.errorCode},message:${it.errorMsg}"
                    )
                }
        }
    }
}