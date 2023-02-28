package com.dian.demo.http.interceptor



import com.dian.demo.ProjectApplication
import com.dian.demo.utils.NetWorkUtil
import okhttp3.Interceptor
import okhttp3.Response


class OfflineCacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!NetWorkUtil.isNetworkAvailable(ProjectApplication.getAppContext())) {
            // 无网络时，设置超时为4周  只对get有用,post没有缓冲
            val maxStale = 60 * 60 * 24 * 28
            response.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                .removeHeader("nyn")
                .build()
        }

        return response
    }
}