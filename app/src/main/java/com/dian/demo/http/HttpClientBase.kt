package com.dian.demo.http

import android.content.Context
import com.dian.demo.http.interceptor.HeadersInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/8/19 5:21 PM
 * @description: Http客户端基底
 * @since: 1.0.0
 */
open class HttpClientBase {
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var retrofit: Retrofit
    private lateinit var requestService: RequestService
    private lateinit var config: HttpClientConfig

    /**
     * 初始化
     */
    protected fun initialize(context: Context, httpClientConfig: HttpClientConfig) {
        config = httpClientConfig
        okHttpClient = buildOkHttpClient(context)
        retrofit = buildRetrofit()
        initRequestService()
    }

    /**
     * 构建OkHttp客户端
     */
    open fun buildOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeout.first, config.connectTimeout.second)
            .readTimeout(config.readTimeout.first, config.readTimeout.second)
            .writeTimeout(config.writeTimeout.first, config.writeTimeout.second)
            .retryOnConnectionFailure(config.retryOnConnectionFailure)
            .cache(config.cache)
            .also { builder ->
                config.headers?.run { builder.addInterceptor(HeadersInterceptor(this)) }
                config.dns?.run { builder.dns(this) }
                config.interceptors.forEach {
                    builder.addInterceptor(it)
                }
                config.netInterceptors.forEach {
                    builder.addNetworkInterceptor(it)
                }
                if (config.openLog) {
                    val logInterceptor = if (config.logger == null) {
                        HttpLoggingInterceptor()
                    } else {
                        HttpLoggingInterceptor(config.logger!!)
                    }
                    logInterceptor.level = HttpLoggingInterceptor.Level.BODY
                    builder.addNetworkInterceptor(logInterceptor)
                }
            }
            .hostnameVerifier({ hostname, session -> true })
            .build()
    }

    /**
     * 构建Retrofit客户端
     */
    open fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                )
            )
            .build()
    }

    /**
     * 初始化请求Service
     */
    private fun initRequestService() {
        requestService = retrofit.create(RequestService::class.java)
    }

    /**
     * 获取OkHttpClient实例
     */
    fun getOkHttpClient() = okHttpClient

    /**
     * 获取Retrofit实例
     */
    fun getRetrofit() = retrofit

    /**
     * 获取请求Service
     */
    fun getRequestService() = requestService

    /**
     * 获取Gson实例
     */
    fun getGson() = config.gson
}