package com.dian.demo.http

import android.content.Context
import com.dian.demo.BuildConfig
import com.dian.demo.ProjectApplication
import com.dian.demo.http.interceptor.*
import com.dian.demo.utils.LogUtil
import com.readystatesoftware.chuck.ChuckInterceptor
import okhttp3.Cache
import java.io.File
import java.lang.reflect.Type

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/3/19 8:10 PM
 * @description: -
 * @since: 1.0.0
 */
class HttpUtils {
    private val httClient by lazy { HttpClient() }
    private val LOG_TAG = "HttpAppUtils>>>"
    private val LOG_DIVIDER = "||================================================================="

    companion object {

        @Volatile
        private var instance: HttpUtils? = null

        @JvmStatic
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: HttpUtils().also { instance = it }
            }
    }


    fun init(context: Context) {
        val config = HttpClientConfig.builder()
            .addInterceptor(ChuckInterceptor(ProjectApplication.getAppContext()))
            .addInterceptor(AddCookieInterceptor())
            .addInterceptor(SaveCookieInterceptor())
            .addInterceptor(CacheInterceptor())
            .addInterceptor(NetCacheInterceptor())
            .addInterceptor(OfflineCacheInterceptor())
            .addInterceptor(ParamsInterceptor())
            .openLog(BuildConfig.DEBUG)
            .setLogger(object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    if (message.contains("--> END") || message.contains("<-- END")) {
                        LogUtil.e(LOG_TAG, "||  " + message)
                        LogUtil.e(LOG_TAG, LOG_DIVIDER)
                    } else if (message.contains("-->") || message.contains("<--")) {
                        LogUtil.e(LOG_TAG, LOG_DIVIDER)
                        LogUtil.e(LOG_TAG, "||  " + message)
                    } else {
                        LogUtil.e(LOG_TAG, "||  " + message)
                    }
                }
            })
            .retryOnConnectionFailure(true)
            .setCache(
                Cache(
                    File(context.cacheDir.toString() + "HttpAppCache"),
                    1024L * 1024 * 100
                )
            )
            .build()
        httClient.init(context, config)
    }

    fun getClient(): HttpClient {
        return httClient
    }

    @JvmOverloads
    suspend fun <T> get(
        url: String,
        headers: Map<String, String>? = null,
        params: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> = httClient.get(url, headers, params, type, isInfoResponse)

    @JvmOverloads
    suspend fun <T> post(
        url: String,
        headers: Map<String, String>? = null,
        params: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> = httClient.postForm(url, headers, params, type, isInfoResponse)
}