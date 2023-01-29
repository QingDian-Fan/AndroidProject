package com.dian.demo.utils

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.dian.demo.R
import com.dian.demo.ui.activity.HomeActivity
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.utils.IntentUtils.openBrowser


object SchemaUtil {
    private const val PAGE_BROWSER = "browser" //浏览器

    private const val PAGE_WEB_VIEW = "webview" //H5界面

    const val PAGE_HOME = "home"

    //msg = "dian://webview?link_url=https://wanandroid.com/"
    fun schemaToPage(
        mContext: Context,
        msg: String,
        title: String = ResourcesUtils.getString(R.string.app_name)
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
                HomeActivity.start(mContext)
            }
        }

    }
}