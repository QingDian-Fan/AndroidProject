package com.demo.project.ui.activity


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.common.http.APIConfig.DEBUG_URL_CONFIG
import com.common.http.APIConfig.getBaseUrl
import com.common.ui.BaseAppBindActivity
import com.common.utils.ActivityManager
import com.common.utils.DomainUtil
import com.common.utils.ResourcesUtil
import com.common.utils.datastore.AppDataStore
import com.common.utils.ext.showAllowStateLoss
import com.common.utils.ext.singleClick
import com.common.weight.titlebar.CommonTitleBar
import com.demo.project.R
import com.demo.project.databinding.ActivityDebugBinding
import com.demo.project.ui.dialog.DebugDialog
import com.demo.project.ui.dialog.LogFileDialog
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DebugActivity : BaseAppBindActivity<ActivityDebugBinding>() {

    companion object {
        @JvmStatic
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, DebugActivity::class.java).apply {
                if (mContext !is Activity) {
                    flags=FLAG_ACTIVITY_NEW_TASK
                }
            }
            mContext.startActivity(intent)
        }
    }

    private var pingJob: Job? = null
    @Volatile
    private var pingProcess: Process? = null


    override fun getLayoutId(): Int = R.layout.activity_debug

    /**
     *  初始化操作
     */
    override fun initialize(savedInstanceState: Bundle?) {
        setPageTitle(ResourcesUtil.getString(R.string.debug_page_title))
        setPageRightIcon(R.mipmap.icon_share)
        getTitleBarView()?.setListener { _, action, _ ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressed()
            } else if (action == CommonTitleBar.ACTION_RIGHT_BUTTON) {

                LogFileDialog.getDialog().showAllowStateLoss(supportFragmentManager,"")
            }
        }
        val debugUrl: String = ResourcesUtil.getString(R.string.debug_base_url)
        val releaseUrl: String = ResourcesUtil.getString(R.string.release_base_url)

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
            startPingTest(
                DomainUtil.getDomainMedium(
                    AppDataStore.getData(
                        DEBUG_URL_CONFIG,
                        releaseUrl
                    )
                )
            )
        }
        binding.cvDebug.singleClick {
            binding.ivDebugCheck.setImageResource(R.mipmap.icon_selected)
            binding.ivReleaseCheck.setImageResource(R.mipmap.icon_unselected)
            AppDataStore.putData(DEBUG_URL_CONFIG, debugUrl)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityManager.getInstance().restartAPP(this@DebugActivity)
            }
        }
        binding.cvRelease.singleClick {
            binding.ivDebugCheck.setImageResource(R.mipmap.icon_unselected)
            binding.ivReleaseCheck.setImageResource(R.mipmap.icon_selected)
            AppDataStore.putData(DEBUG_URL_CONFIG, releaseUrl)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityManager.getInstance().restartAPP(this@DebugActivity)
            }
        }
        binding.btnSchema.singleClick {
            DebugDialog().showAllowStateLoss(supportFragmentManager,"")
        }

    }

    private fun startPingTest(ipAddress: String) {
        pingJob?.cancel()
        pingProcess?.destroy()
        pingJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                pingAddress(ipAddress)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                appendNetworkMessage(e.message ?: e.toString())
            }
        }
    }

    private suspend fun pingAddress(ipAddress: String) {
        val process = Runtime.getRuntime().exec("ping -c 8 -i 1 -s 64 $ipAddress")
        pingProcess = process
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { successReader ->
                readPingLines(successReader)
            }
            BufferedReader(InputStreamReader(process.errorStream)).use { errorReader ->
                readPingLines(errorReader)
            }
            process.waitFor()
        } finally {
            process.destroy()
            if (pingProcess == process) {
                pingProcess = null
            }
        }
    }

    private suspend fun readPingLines(reader: BufferedReader) {
        while (currentCoroutineContext().isActive) {
            val line = reader.readLine() ?: break
            appendNetworkMessage(line.trimIndent())
        }
    }

    private suspend fun appendNetworkMessage(message: String) {
        withContext(Dispatchers.Main) {
            withBinding {
                tvNetworkMessage.append(message)
                tvNetworkMessage.append("\n")
            }
        }
    }

    override fun onDestroy() {
        pingJob?.cancel()
        pingProcess?.destroy()
        pingJob = null
        pingProcess = null
        super.onDestroy()
    }

}
