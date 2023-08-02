package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityScanResultBinding
import com.dian.demo.http.WebSocketClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class ScanResultActivity : BaseAppBindActivity<ActivityScanResultBinding>() {
    companion object {
        fun start(mContext: Context, content: String) {
            val intent = Intent()
            intent.putExtra("result", content)
            intent.setClass(mContext, ScanResultActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_scan_result

    override fun initialize(savedInstanceState: Bundle?) {

        getTitleBarView().setLeftIcon(R.mipmap.ic_close)
        val resultString = intent.getStringExtra("result")
        binding.tvResult.text = resultString

        initData()
    }
    private val wsListener = object :WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            super.onMessage(webSocket, bytes)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
        }


        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
        }


        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
        }
    }
    private fun initData() {
        val wsClient = WebSocketClient()
        wsClient.start(wsListener)
       // wsClient.getWebSocket().send()
    }
}