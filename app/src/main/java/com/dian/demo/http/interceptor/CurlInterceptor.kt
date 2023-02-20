package com.dian.demo.http.interceptor


import com.dian.demo.http.interceptor.curl.Configuration
import com.dian.demo.http.interceptor.curl.CurlCommandGenerator
import com.dian.demo.utils.LogUtil
import okhttp3.Interceptor
import okhttp3.Response

class CurlInterceptor @JvmOverloads constructor(configuration: Configuration = Configuration()) : Interceptor {

    private val curlGenerator = CurlCommandGenerator(configuration)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val curl = curlGenerator.generate(request)
        LogUtil.e("HttpAppUtils>>>",curl)

        return chain.proceed(request)
    }
}