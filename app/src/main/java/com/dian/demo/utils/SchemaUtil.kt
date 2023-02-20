package com.dian.demo.utils

import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.text.TextUtils
import com.dian.demo.R
import com.dian.demo.ui.activity.DemoActivity
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.utils.IntentUtil.openBrowser


object SchemaUtil {
    private const val PAGE_BROWSER = "browser" //浏览器

    private const val PAGE_WEB_VIEW = "webview" //H5界面

    private const val PAGE_HOME = "home"

    private const val PAGE_SETTING ="setting"

    private const val PAGE_INSTALL = "install"

    //msg = "dian://webview?link_url=https://wanandroid.com/"
    fun schemaToPage(
        mContext: Context,
        msg: String,
        title: String = ResourcesUtil.getString(R.string.app_name)
    ) {
        if (TextUtils.isEmpty(msg)) {
            return
        }
        val uri: Uri = Uri.parse(msg)
        val host: String = uri.host ?: return
        if (PAGE_BROWSER == host) {
            val linkUrl = uri.getQueryParameter("link_url")
            openBrowser(mContext, linkUrl)
            return
        }
        when (host) {
            PAGE_WEB_VIEW -> {
                val linkUrl = uri.getQueryParameter("link_url")
                WebExplorerActivity.start(mContext, linkUrl!!, title)
            }
            PAGE_HOME -> {
                DemoActivity.start(mContext)
            }
            PAGE_SETTING->{
                IntentUtil.goToAppSetting(mContext)
            }
            PAGE_INSTALL->{
                val filePath = uri.getQueryParameter("filePath")
                filePath?.let {
                    IntentUtil.installedApp(mContext,filePath)
                }
            }
        }

    }
}