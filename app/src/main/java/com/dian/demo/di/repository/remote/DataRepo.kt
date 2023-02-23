package com.dian.demo.di.repository.remote

import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.ListData
import com.dian.demo.di.model.UserBean
import com.dian.demo.http.ResponseHolder


interface DataRepo {
    suspend fun getArticleList(page:Int): ResponseHolder<ListData<ArticleBean>>

    suspend fun doLogin(userName:String,password:String):ResponseHolder<UserBean>
}