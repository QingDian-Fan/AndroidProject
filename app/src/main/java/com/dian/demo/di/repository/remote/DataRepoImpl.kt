package com.dian.demo.di.repository.remote

import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.ListData
import com.dian.demo.http.HttpUtils
import com.dian.demo.http.ResponseHolder
import com.google.gson.reflect.TypeToken
import okhttp3.internal.format
import com.dian.demo.http.Result
import com.squareup.moshi.Types


class DataRepoImpl : DataRepo {
    override suspend fun getArticleList(page: Int): ResponseHolder<ListData<ArticleBean>> {

        return HttpUtils.getInstance().get(
            url = format("article/list/%d/json",page),
            type = Types.newParameterizedType(Result::class.java, Types.newParameterizedType(ListData::class.java, ArticleBean::class.java))
        )
    }
}