package com.dian.demo.di.repository.remote

import com.dian.demo.di.model.*
import com.dian.demo.http.HttpUtils
import com.dian.demo.http.ResponseHolder
import com.google.gson.reflect.TypeToken
import okhttp3.internal.format
import com.dian.demo.http.Result
import com.squareup.moshi.Types


class DataRepoImpl : DataRepo {
    val map = hashMapOf<String, String>()


    /**
     * 登录
     * url :  https://www.wanandroid.com/user/login
     * 方法 : POST
     * 参数 : username，password
     */
    override suspend fun doLogin(userName: String, password: String): ResponseHolder<UserBean> {
        map.clear()
        map["username"] = userName
        map["password"] = password

        return HttpUtils.getInstance().post(
            url = "user/login",
            params = map,
            type = Types.newParameterizedType(Result::class.java, UserBean::class.java)
        )
    }


    /**
     * 注册
     * url: https://www.wanandroid.com/user/register
     * 方法：POST
     * 参数:username,password,repassword
     */
    override suspend fun doRegister(
        userName: String,
        password: String,
        rePassword: String
    ): ResponseHolder<UserBean> {
        map.clear()
        map["username"] = userName
        map["password"] = password
        map["repassword"] = rePassword
        return HttpUtils.getInstance().post(
            url = "user/register",
            params = map,
            type = Types.newParameterizedType(Result::class.java, UserBean::class.java)
        )
    }

    /**
     * url : https://www.wanandroid.com/user/logout/json
     * 方法: GET
     */
    override suspend fun doLoginOut(): ResponseHolder<Result<UserBean>> {
        return HttpUtils.getInstance().get(
            url = "user/logout/json",
            type = Types.newParameterizedType(Result::class.java, Any::class.java)
        )
    }

    /**
     * https://www.wanandroid.com/banner/json
     * 方法：GET
     * 参数：无
     */
    override suspend fun getBanner(): ResponseHolder<List<BannerBean>> {
        return HttpUtils.getInstance().get(
            url = "banner/json",
            type = Types.newParameterizedType(
                Result::class.java,
                Types.newParameterizedType(List::class.java, BannerBean::class.java)
            )
        )
    }


    /**
     * https://www.wanandroid.com/article/list/0/json
     * 方法：GET
     * 参数：页码，拼接在连接中，从0开始。
     */
    override suspend fun getArticleList(page: Int): ResponseHolder<ListData<ArticleBean>> {

        return HttpUtils.getInstance().get(
            url = format("article/list/%d/json", page),
            type = Types.newParameterizedType(
                Result::class.java,
                Types.newParameterizedType(ListData::class.java, ArticleBean::class.java)
            )
        )
    }

    override suspend fun getAnswersList(page: Int): ResponseHolder<ListData<ArticleBean>> {
        return HttpUtils.getInstance().get(
            url = format("wenda/list/%d/json", page),
            type = Types.newParameterizedType(
                Result::class.java,
                Types.newParameterizedType(ListData::class.java, ArticleBean::class.java)
            )
        )
    }

    /**
     * https://www.wanandroid.com/lg/todo/v2/list/页码/json
     * 页码从1开始，拼接在url 上
     * status 状态， 1-完成；0未完成; 默认全部展示；
     * type 创建时传入的类型, 默认全部展示
     * priority 创建时传入的优先级；默认全部展示
     * orderby 1:完成日期顺序；2.完成日期逆序；3.创建日期顺序；4.创建日期逆序(默认)；
     */
    override suspend fun getTodoList(page: Int): ResponseHolder<ListData<TodoData>> {
        return HttpUtils.getInstance().get(
            url = "lg/todo/v2/list/$page/json",
            type = Types.newParameterizedType(
                Result::class.java,
                Types.newParameterizedType(ListData::class.java, TodoData::class.java)
            )
        )
    }

    override suspend fun getUserInfo(): ResponseHolder<UserInfo> {
        return HttpUtils.getInstance().get(
            url= "user/lg/userinfo/json",
            type = Types.newParameterizedType(
                Result::class.java,
                UserInfo::class.java
            )
        )
    }

    override suspend fun getSearchHotHistoryRecord(): ResponseHolder<List<SearchRecord>> {
       return HttpUtils.getInstance().get(
           url = "hotkey/json",
           type = Types.newParameterizedType(
               Result::class.java,
                       Types.newParameterizedType(List::class.java, SearchRecord::class.java)
           )
       )
    }

    override suspend fun getSearchList(page: Int,keyword: String): ResponseHolder<ListData<ArticleBean>> {
        map.clear()
        map["k"] = keyword
        return HttpUtils.getInstance().post(
            url = format("article/query/%d/json", page),
            params = map,
            type = Types.newParameterizedType(
                Result::class.java,
                Types.newParameterizedType(ListData::class.java, ArticleBean::class.java)
            )
        )
    }
}