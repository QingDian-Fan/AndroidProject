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

        getTitleBarView()?.setLeftIcon(R.mipmap.ic_close)
        val resultString = intent.getStringExtra("result")
        binding.tvResult.text = resultString

    }

}