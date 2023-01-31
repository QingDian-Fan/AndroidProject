package com.dian.demo.http.interceptor



import com.dian.demo.R
import com.dian.demo.utils.ResourcesUtil
import okhttp3.Interceptor
import okhttp3.Response

class ParamsInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val modifiedUrl = originalRequest.url.newBuilder()
            .addQueryParameter("language", ResourcesUtil.getString(R.string.language))
            .build()
        val request = originalRequest.newBuilder().url(modifiedUrl).build()
        return chain.proceed(request)
    }
}