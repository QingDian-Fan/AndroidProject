package com.dian.demo.http

import android.util.Log
import com.dian.demo.utils.LogUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketClient {
    private var request: Request
    private var client: OkHttpClient
    private var webSocket: WebSocket? = null
    private var connectTimeOut = 10
    private val baseUrl: String = ""

    constructor() {
         client = OkHttpClient().newBuilder().retryOnConnectionFailure(true)
             .connectTimeout(connectTimeOut.toLong(), TimeUnit.SECONDS)
             .build()
        request = Request.Builder()
            .url(baseUrl)
            .build()
    }

    constructor(timeOut: Int) {
        connectTimeOut = timeOut
        client = OkHttpClient().newBuilder().retryOnConnectionFailure(true)
            .connectTimeout(connectTimeOut.toLong(), TimeUnit.SECONDS)
            .build()
        request = Request.Builder()
            .url(baseUrl)
            .build()
    }

    constructor(url: String) {
        client = OkHttpClient().newBuilder().retryOnConnectionFailure(true)
            .connectTimeout(connectTimeOut.toLong(), TimeUnit.SECONDS)
            .build()
        request = Request.Builder()
            .url(url)
            .build()
    }

    constructor(timeOut: Int, url: String) {
        connectTimeOut = timeOut
        client = OkHttpClient().newBuilder().retryOnConnectionFailure(true)
            .connectTimeout(connectTimeOut.toLong(), TimeUnit.SECONDS)
            .build()
        request = Request.Builder()
            .url(url)
            .build()
    }

    fun getWebSocket(): WebSocket? {
        return webSocket
    }

    private var cancelSocket: WebSocket? = null

    fun getCancelSocket(): WebSocket? {
        return cancelSocket
    }

    fun start(listener: WebSocketListener?) {
        cancelSocket = webSocket
        client.dispatcher.cancelAll()
        webSocket = client.newWebSocket(request, listener!!)
        LogUtil.e("webSocket.id==$webSocket")
        if (cancelSocket != null) {
            LogUtil.e("webSocket.id==" + webSocket.toString() + "cancelSocket==" + cancelSocket.toString())
        }
    }

    fun stop() {
        if (webSocket != null) {
            webSocket?.close(1000, null)
        }
        client.dispatcher.executorService.shutdown()
    }
}