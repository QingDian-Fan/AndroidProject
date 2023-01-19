package com.dian.demo.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import com.demo.project.config.Constant.DEBUG_URL_CONFIG
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.config.AppConfig.getBaseUrl
import com.dian.demo.databinding.ActivityDebugBinding
import com.dian.demo.ui.dialog.DebugDialog
import com.dian.demo.ui.titlebar.CommonTitleBar
import com.dian.demo.utils.DomainUtil
import com.dian.demo.utils.ExceptionHandlerUtil
import com.dian.demo.utils.PreferenceUtils
import com.dian.demo.utils.ResourcesUtils
import com.dian.demo.utils.ext.singleClick
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.concurrent.thread


class DebugActivity : BaseAppBindActivity<ActivityDebugBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, DebugActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            val resultMsg = msg.obj as String
            binding.tvNetworkMessage.append(resultMsg)
        }
    }


    override fun getLayoutId(): Int = R.layout.activity_debug

    /**
     *  初始化操作
     */
    override fun initialize(savedInstanceState: Bundle?) {
        setPageTitle(ResourcesUtils.getString(R.string.debug_page_title))
        setPageRightIcon(R.mipmap.icon_share)
        getTitleBarView().setListener { _, action, _ ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressed()
            } else if (action == CommonTitleBar.ACTION_RIGHT_BUTTON) {
                ExceptionHandlerUtil.doShareFile()
            }
        }
        val debugUrl: String = ResourcesUtils.getString(R.string.debug_base_url)
        val releaseUrl: String = ResourcesUtils.getString(R.string.release_base_url)

        val urlString = getBaseUrl()
        if (urlString == debugUrl) {
            binding.ivDebugCheck.setImageResource(R.mipmap.icon_selected)
            binding.ivReleaseCheck.setImageResource(R.mipmap.icon_unselected)
        } else {
            binding.ivDebugCheck.setImageResource(R.mipmap.icon_unselected)
            binding.ivReleaseCheck.setImageResource(R.mipmap.icon_selected)
        }
        binding.btnNetworkTest.singleClick {
            binding.tvNetworkMessage.text = null
            thread {
                pingAddress(
                    DomainUtil.getDomainMedium(
                        PreferenceUtils.getString(
                            DEBUG_URL_CONFIG,
                            debugUrl
                        )
                    )
                )
            }
        }
        binding.cvDebug.singleClick {
            binding.ivDebugCheck.setImageResource(R.mipmap.icon_selected)
            binding.ivReleaseCheck.setImageResource(R.mipmap.icon_unselected)
            PreferenceUtils.putString(DEBUG_URL_CONFIG, debugUrl)
        }
        binding.cvRelease.singleClick {
            binding.ivDebugCheck.setImageResource(R.mipmap.icon_unselected)
            binding.ivReleaseCheck.setImageResource(R.mipmap.icon_selected)
            PreferenceUtils.putString(DEBUG_URL_CONFIG, releaseUrl)
        }
        binding.btnSchema.singleClick {
              val debugDialog = DebugDialog()
             debugDialog.show(supportFragmentManager, "")


        }
    }

    @Throws(Exception::class)
    fun pingAddress(ipAddress: String) {
        val process = Runtime.getRuntime().exec("ping -c 8 -i 1 -s 64 $ipAddress")
        val ins: InputStream = process!!.inputStream
        val successReader = BufferedReader(InputStreamReader(ins))
        val errorReader = BufferedReader(InputStreamReader(process.errorStream))
        var lineStr: String? = null
        while (successReader.readLine().also { if (it != null) lineStr = it } != null
            && lineStr != null) {
            val msg: Message = mHandler.obtainMessage()
            msg.obj = """ $lineStr """.trimIndent()
            msg.what = 10
            msg.sendToTarget()
        }
        while (errorReader.readLine().also { lineStr = it } != null) {
            val msg: Message = mHandler.obtainMessage()
            msg.obj = lineStr?.trimIndent()
            msg.what = 10
            msg.sendToTarget()
        }
        successReader.close()
        errorReader.close()
        process.destroy()
    }

}