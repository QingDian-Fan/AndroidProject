package com.demo.project.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.view.Gravity
import com.common.utils.NetWorkUtil
import com.common.utils.ToastUtil

/**
 * 监听网络状态的广播接收者
 * 例如：可以收到 "网络不给力"
 */
class NetworkStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            if (!NetWorkUtil.isNetworkAvailable()) {
                ToastUtil.showToast(context, "网络不给力", false, Gravity.CENTER)
            }
        }
    }
}