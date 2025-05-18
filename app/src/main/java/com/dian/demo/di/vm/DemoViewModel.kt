package com.dian.demo.di.vm

import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.ArticleEntity
import com.dian.demo.di.repository.local.DataBaseManager
import com.dian.demo.utils.aop.CheckNet

class DemoViewModel : BaseViewModel() {

    @CheckNet
    fun getArticleList(page: Int){
        launchOnUI {
            repo.getArticleList(page)
                .onCompletion {
                    showLoadingView(false)
                }
                .onSuccess {
                    val articleList = mutableListOf<ArticleEntity>()
                    it?.datas?.forEach {
                        articleList.add(ArticleEntity(id=it.id, link = it.link, title = it.title, superChapterName = it.superChapterName))
                    }
                    localRepo.deleteArticleEntities()
                    localRepo.insertArticleEntities(articleList)
                    showToast("请求成功")
                }
                .onFailure { _, _ ->
                    showErrorView(true)
                }
                .onCatch {
                    showErrorView(true)
                }
        }

    }

    @CheckNet
    fun doLogin(username:String,password:String){
        launchOnUI {
            repo.doLogin(username,password)
                .onSuccess {
                    showToast("请求成功")
                }
        }
    }

}