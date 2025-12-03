package com.dian.demo.di.repository.remote

import com.dian.demo.di.model.*
import com.dian.demo.http.ResponseHolder
import com.dian.demo.http.Result


interface DataRepo {


    suspend fun doLogin(userName:String,password:String):ResponseHolder<UserBean>

    suspend fun doRegister(userName:String,password:String,rePassword:String):ResponseHolder<UserBean>

    suspend fun doLoginOut():ResponseHolder<Result<UserBean>>

    suspend fun getBanner():ResponseHolder<List<BannerBean>>

    suspend fun getArticleList(page:Int): ResponseHolder<ListData<ArticleBean>>
    suspend fun getAnswersList(page:Int): ResponseHolder<ListData<ArticleBean>>
    suspend fun getMineShareList(page:Int): ResponseHolder<ShareArticle>
    suspend fun getMineCollectList(page:Int): ResponseHolder<ListData<ArticleBean>>

    suspend fun getTodoList(page:Int): ResponseHolder<ListData<TodoData>>

    suspend fun getUserInfo(): ResponseHolder<UserInfo>

    suspend fun getSearchHotHistoryRecord(): ResponseHolder<List<SearchRecord>>

    suspend fun getSearchList(page:Int,keyword: String): ResponseHolder<ListData<ArticleBean>>

    suspend fun getSetupDataList(): ResponseHolder<List<SetUpData>>

    suspend fun getNavigationDataList(): ResponseHolder<List<SetUpData>>
    suspend fun getKnowledgeDetailData(page:Int,cid: String): ResponseHolder<ListData<ArticleBean>>
    suspend fun getCoinRecordList(page:Int): ResponseHolder<ListData<CoinCount>>

}