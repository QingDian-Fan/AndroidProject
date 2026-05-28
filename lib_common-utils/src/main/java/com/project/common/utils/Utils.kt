package com.project.common.utils

import android.content.Context
import android.text.TextUtils
import com.dian.demo.ProjectApplication
import com.meituan.android.walle.WalleChannelReader

object Utils {

    var channel: String? = null
    fun getChannel(context: Context=ProjectApplication.getAppContext()): String {
        if (channel.isNullOrBlank()) {
            channel = WalleChannelReader.getChannel(context) ?: "official"
        }
        return channel!!
    }
}