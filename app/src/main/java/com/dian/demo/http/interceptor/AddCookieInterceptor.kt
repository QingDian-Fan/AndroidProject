package com.dian.demo.http.interceptor

import com.dian.demo.http.cookie.HttpConstant
import com.dian.demo.utils.datastore.AppDataStore
import okhttp3.Interceptor
import okhttp3.Response


class AddCookieInterceptor : Interceptor {


    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        val builder = request.newBuilder()

        builder.addHeader("Content-type", "application/json; charset=utf-8")

        val domain = request.url.host
        val url = request.url.toString()
        if (domain.isNotEmpty() && (url.contains(HttpConstant.COLLECTIONS_WEBSITE)
                    || url.contains(HttpConstant.UNCOLLECTIONS_WEBSITE)
                    || url.contains(HttpConstant.ARTICLE_WEBSITE)
                    || url.contains(HttpConstant.TODO_WEBSITE)
                    || url.contains(HttpConstant.COIN_WEBSITE))
        ) {

            val cookies = AppDataStore.getData(domain,"")
            val cookie: String = cookies.ifEmpty { "" }
            if (cookie.isNotEmpty()) {
                // 将 Cookie 添加到请求头
                builder.addHeader(HttpConstant.COOKIE_NAME, cookie)
            }
        }

        return chain.proceed(builder.build())
    }

}