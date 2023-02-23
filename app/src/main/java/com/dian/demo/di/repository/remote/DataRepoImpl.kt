package com.dian.demo.di.repository.remote

import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.ListData
import com.dian.demo.di.model.UserBean
import com.dian.demo.http.HttpUtils
import com.dian.demo.http.ResponseHolder
import com.google.gson.reflect.TypeToken
import okhttp3.internal.format
import com.dian.demo.http.Result
import com.squareup.moshi.Types


class DataRepoImpl : DataRepo {
    override suspend fun getArticleList(page: Int): ResponseHolder<ListData<ArticleBean>> {

        return HttpUtils.getInstance().get(
            url = format("article/list/%d/json", page),
            type = Types.newParameterizedType(
                Result::class.java,
                Types.newParameterizedType(ListData::class.java, ArticleBean::class.java)
            )
        )
    }

    override suspend fun doLogin(userName: String, password: String): ResponseHolder<UserBean> {
        val map = hashMapOf<String, String>()
        map["username"] = userName
        map["password"] = password

        return HttpUtils.getInstance().post(
            url = "user/login",
            params = map,
            type = Types.newParameterizedType(Result::class.java, UserBean::class.java)
        )
    }
}