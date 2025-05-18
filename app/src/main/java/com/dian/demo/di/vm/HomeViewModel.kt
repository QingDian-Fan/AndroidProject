package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.ArticleEntity
import com.dian.demo.di.model.BannerBean
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.aop.CheckNet

class HomeViewModel : BaseViewModel() {


    val bannerData = MutableLiveData<List<BannerBean>>()
    val articleData = MutableLiveData<List<ArticleBean>>()

    @CheckNet
    fun getBannerList() {
        launchOnUI {
            repo.getBanner()
                .onCompletion {
                    LogUtil.e("TAG----->Request", "banner请求结束")
                }
                .onSuccess {
                    it?.let {
                        bannerData.value = it
                    }
                }
                .onFailure { code, message ->
                    LogUtil.e("TAG----->Request", "banner失败：code：${code},message:${message}")
                }
                .onCatch {
                    LogUtil.e(
                        "TAG----->Request",
                        "banner失败：code：${it.errorCode},message:${it.errorMsg}"
                    )
                }
        }
    }

    @CheckNet
    fun getArticleList(page: Int) {
        launchOnUI {
            repo.getArticleList(page)
                .onCompletion {
                    LogUtil.e("TAG----->Request", "articleList请求结束")
                    showLoadingView(false)
                }
                .onSuccess { it ->
                    it?.datas?.let {
                        articleData.value = it
                    }
                    showToast("请求成功")
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