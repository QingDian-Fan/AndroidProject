package com.dian.demo.http

import com.dian.demo.config.AppConfig.getBaseUrl
import com.dian.demo.http.gson.GsonFactory
import com.google.gson.Gson
import okhttp3.Cache
import okhttp3.Dns
import okhttp3.Interceptor
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/8/8 5:00 下午
 * @description: 网络请求客户端配置类
 * @since: 1.0.0
 */
data class HttpClientConfig(
    // BaseUrl
    var baseUrl:String = getBaseUrl(),
    // 连接超时时间
    var connectTimeout: Pair<Long, TimeUnit> = Pair(60L, TimeUnit.SECONDS),
    // 读取超时时间
    var readTimeout: Pair<Long, TimeUnit> = Pair(60L, TimeUnit.SECONDS),
    // 写入超时时间
    var writeTimeout: Pair<Long, TimeUnit> = Pair(60L, TimeUnit.SECONDS),
    // 请求头
    var headers: Map<String, String>? = null,
    // 缓存
    var cache: Cache? = null,
    // DNS
    var dns: Dns? = null,
    /**
     * 应用拦截器
     *
     * 1.不需要担心中间过程的响应,如重定向和重试
     * 2.总是只调用一次,即使HTTP响应是从缓存中获取
     * 3.观察应用程序的初衷. 不关心OkHttp注入的头信息如: If-None-Match
     * 4.允许短路而不调用 Chain.proceed(),即中止调用
     * 5.允许重试,使 Chain.proceed()调用多次
     */
    val interceptors: ArrayList<Interceptor> = arrayListOf(),
    /**
     * 网络拦截器
     *
     * 1.能够操作中间过程的响应,如重定向和重试
     * 2.当网络短路而返回缓存响应时不被调用
     * 3.只观察在网络上传输的数据
     * 4.携带请求来访问连接
     */
    val netInterceptors: ArrayList<Interceptor> = arrayListOf(),
    // 是否在连接失败后发起重连
    var retryOnConnectionFailure: Boolean = true,
    // 是否打开日志
    var openLog: Boolean = false,
    // 自定义日志打印
    var logger: HttpLoggingInterceptor.Logger? = null,
    // Gson
    var gson: Gson = GsonFactory.getSingletonGson(),
) {
    companion object {
        @JvmStatic
        fun builder() = HttpClientConfigBuilder()
    }
}

class HttpClientConfigBuilder {

    private val config: HttpClientConfig = HttpClientConfig()

    fun setBaseUrl(url: String): HttpClientConfigBuilder {
        config.baseUrl = url
        return this
    }

    fun setConnectTimeout(time: Long, timeUnit: TimeUnit): HttpClientConfigBuilder {
        config.connectTimeout = Pair(time, timeUnit)
        return this
    }

    fun setReadTimeout(time: Long, timeUnit: TimeUnit): HttpClientConfigBuilder {
        config.readTimeout = Pair(time, timeUnit)
        return this
    }

    fun setWriteTimeout(time: Long, timeUnit: TimeUnit): HttpClientConfigBuilder {
        config.writeTimeout = Pair(time, timeUnit)
        return this
    }

    fun setHeaders(headers: Map<String, String>): HttpClientConfigBuilder {
        config.headers = headers
        return this
    }

    fun setCache(cache: Cache): HttpClientConfigBuilder {
        config.cache = cache
        return this
    }

    fun setDns(dns: Dns): HttpClientConfigBuilder {
        config.dns = dns
        return this
    }

    fun addInterceptor(interceptor: Interceptor): HttpClientConfigBuilder {
        config.interceptors.add(interceptor)
        return this
    }

    fun addNetworkInterceptor(interceptor: Interceptor): HttpClientConfigBuilder {
        config.netInterceptors.add(interceptor)
        return this
    }

    fun retryOnConnectionFailure(isRetry: Boolean): HttpClientConfigBuilder {
        config.retryOnConnectionFailure = isRetry
        return this
    }

    fun openLog(open: Boolean): HttpClientConfigBuilder {
        config.openLog = open
        return this
    }

    fun setLogger(logger: HttpLoggingInterceptor.Logger): HttpClientConfigBuilder {
        config.logger = logger
        return this
    }

    fun setGson(gson: Gson): HttpClientConfigBuilder {
        config.gson = gson
        return this
    }

    fun build(): HttpClientConfig = config
}