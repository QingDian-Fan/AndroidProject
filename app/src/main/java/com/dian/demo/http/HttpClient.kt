package com.dian.demo.http

import android.content.Context
import android.net.ParseException
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.webkit.MimeTypeMap
import com.dian.demo.ProjectApplication
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.ListData
import com.dian.demo.utils.MoshiUtil
import com.dian.demo.utils.ToastUtil
import com.google.gson.JsonParseException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import org.json.JSONException
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.InterruptedIOException
import java.lang.reflect.Type
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/8/8 4:59 下午
 * @description: Http客户端
 * @since: 1.0.0
 */
open class HttpClient : HttpClientBase() {

    fun init(context: Context, httpClientConfig: HttpClientConfig) {
        initialize(context, httpClientConfig)
    }

    /**
     * Http Get
     */
    @JvmOverloads
    suspend fun <T> get(
        url: String,
        headers: Map<String, String>? = null,
        params: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> =
        request(type, isInfoResponse) { it.get(url, headers ?: mapOf(), params ?: mapOf()) }


    /**
     * Http Post
     * Data post in form
     */
    @JvmOverloads
    suspend fun <T> postForm(
        url: String,
        headers: Map<String, String>? = null,
        params: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> =
        request(type, isInfoResponse) { it.postForm(url, headers ?: mapOf(), params ?: mapOf()) }

    /**
     * Http Post
     * Data post in json
     */
    @JvmOverloads
    suspend fun <T> postJson(
        url: String,
        headers: Map<String, String>? = null,
        content: Map<String, Any>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> {
        var contentString = ""
        if (!content.isNullOrEmpty()) {
          contentString =MoshiUtil.objectsMapToJson(String::class.java,Any::class.java,content)
        }
        return postJsonString(url, headers, contentString, type, isInfoResponse)
    }

    @JvmOverloads
    suspend fun <T> postJsonString(
        url: String,
        headers: Map<String, String>? = null,
        content: String? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> =
        request(type, isInfoResponse) { it.postJson(url, headers ?: mapOf(), content ?: "") }

    /**
     * Support Multipart body for POST
     */
    @JvmOverloads
    suspend fun <T> postMultipart(
        url: String,
        headers: Map<String, String>? = null,
        params: Map<String, Any>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> = request(type, isInfoResponse) {
        val mb = MultipartBody.Builder().setType(MultipartBody.FORM)
        params?.forEach {
            if (it.value is File) {
                val file = it.value as File
                var mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.absolutePath))
                if (mimeType.isNullOrBlank()) {
                    mimeType = "file/*"
                }
                mb.addFormDataPart(
                    it.key,
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
            } else {
                mb.addFormDataPart(it.key, it.value.toString())
            }
        }
        it.postOrigin(url, headers ?: mapOf(), mb.build())
    }


    @JvmOverloads
    suspend fun <T> put(
        url: String,
        headers: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> = request(type, isInfoResponse) { it.put(url, headers ?: mapOf()) }

    @JvmOverloads
    suspend fun <T> delete(
        url: String,
        headers: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> = request(type, isInfoResponse) { it.delete(url, headers ?: mapOf()) }

    @JvmOverloads
    suspend fun <T> head(
        url: String,
        headers: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> = request(type, isInfoResponse) { it.head(url, headers ?: mapOf()) }

    @JvmOverloads
    suspend fun <T> options(
        url: String,
        headers: Map<String, String>? = null,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> = request(type, isInfoResponse) { it.options(url, headers ?: mapOf()) }

    suspend fun downloadFile(url: String): Response<ResponseBody>? {
        try {
            return getRequestService().downloadFile(url)
        } catch (cause: Throwable) {
            return null
        }
    }

    /**
     * 建议使用此方法发起网络请求
     * 因为协程中出现异常时，会直接抛出异常，所以使用try...catch方法捕获异常
     */
    open suspend fun <T> request(
        type: Type,
        isInfoResponse: Boolean = true,
        call: suspend (service: RequestService) -> Response<String>
    ): ResponseHolder<T> {
        return try {
            val response = call.invoke(getRequestService())
            parseResponse(response, type, isInfoResponse)
        } catch (cause: Throwable) {
            val httpError = catchException(cause)
            ResponseHolder.Error(httpError)
        }
    }

    /**
     * 发起网络请求，返回Flow
     * 使用普通的协程访问已经足够满足大部分请求，此处使用Flow请求仅仅作为一个扩展
     * 如需大规模使用Flow，可以按照上述request方式进行扩充
     */
    @ExperimentalCoroutinesApi
    open fun <T> requestFlow(
        type: Type,
        isInfoResponse: Boolean = true,
        call: suspend (service: RequestService) -> Response<String>
    ): Flow<ResponseHolder<T>> {
        return try {
            flow {
                val response = call.invoke(getRequestService())
                emit(parseResponse(response, type, isInfoResponse))
            }
        } catch (cause: Throwable) {
            flow {
                val httpError = catchException(cause)
                emit(ResponseHolder.Error(httpError))
            }
        }
    }

    /**
     * 解析请求返回的Response
     */
    open fun <T> parseResponse(
        response: Response<String>,
        type: Type,
        isInfoResponse: Boolean = true
    ): ResponseHolder<T> {
        try {
            return if (response.isSuccessful && response.body() != null) {
                // 请求成功
                if (isInfoResponse) {
                    resolveInfoResponse(response, type)
                } else {
                    resolveNoInfoResponse(response, type)
                }
            } else {
                // 请求失败
                resolveFailedResponse(response)
            }
        } catch (cause: Throwable) {
            val httpError = catchException(cause)
            httpError.httpCode = response.code()
            return ResponseHolder.Error(httpError)
        }
    }

    /**
     * 解析成功的网络请求返回的响应，InfoResponse形式
     */
    open fun <T> resolveInfoResponse(
        response: Response<String>,
        type: Type
    ): ResponseHolder<T> {
        val resp = getMoshi().adapter<Result<T>>(type).fromJson(response.body()!!)!!
        if (resp.isSuccessful()) {
            return ResponseHolder.Success(resp.data)
        } else {
            // 请求成功，返回失败响应
            if (resp.isNotLogin()) {

                //SchemaUtils.toStart(ProjectApplication.getAppContext(), "dian://login")
                ToastUtil.showToast(
                    ProjectApplication.getAppContext(),
                    resp.errorMsg,
                    false,
                    gravity = Gravity.CENTER
                )
            }
            return ResponseHolder.Failure(resp.errorCode, resp.errorMsg)
        }
    }

    /**
     * 解析成功的网络请求返回的响应，非InfoResponse形式
     */
    open fun <T> resolveNoInfoResponse(
        response: Response<String>,
        type: Type
    ): ResponseHolder<T> {
        val resp = getMoshi().adapter<Result<T>>(type).fromJson(response.body()!!)!!
        return ResponseHolder.Success(resp.data)
    }

    /**
     * 解析失败的网络请求返回的响应
     */
    open fun resolveFailedResponse(response: Response<String>): ResponseHolder<Nothing> {
        val errorCode = response.raw().code
        val errorMsg = response.raw().message
        val httpError = HttpError(
            httpCode = response.code(),
            errorCode = errorCode,
            errorMsg = errorMsg
        )
        return ResponseHolder.Error(httpError)
    }

    /**
     * 捕获异常
     */
    open fun catchException(cause: Throwable): HttpError {
        Log.e("Error--->", "网络请求：message:${cause.message}")
        return when (cause) {
            is ConnectException,
            is UnknownHostException -> HttpError(
                errorCode = HttpError.CONNECT_ERROR,
                errorMsg = "服务器连接失败",
                cause = cause
            )
            is InterruptedIOException -> HttpError(
                errorCode = HttpError.CONNECT_TIMEOUT,
                errorMsg = "网络请求超时",
                cause = cause
            )
            is HttpException -> HttpError(
                errorCode = HttpError.BAD_NETWORK,
                errorMsg = "网络请求出错",
                cause = cause
            )
            is JsonParseException,
            is JSONException,
            is ParseException,
            is ClassCastException -> HttpError(
                errorCode = HttpError.PARSE_ERROR,
                errorMsg = "数据解析失败",
                cause = cause
            )
            is CancellationException -> HttpError(
                errorCode = HttpError.CANCEL_REQUEST,
                errorMsg = "",
                cause = cause
            )
            else -> HttpError(
                errorCode = HttpError.UNKNOW_ERROR,
                errorMsg = if (TextUtils.isEmpty(cause.message)) {
                    "未知错误"
                } else {
                    cause.message!!
                },
                cause = cause
            )
        }
    }
}

