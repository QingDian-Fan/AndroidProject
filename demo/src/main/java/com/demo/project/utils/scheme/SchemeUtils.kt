package com.demo.project.utils.scheme

import android.content.Context
import android.net.Uri
import com.common.utils.LogUtil
import com.demo.project.ui.activity.CameraActivity
import com.demo.project.ui.activity.DebugActivity
import com.demo.project.ui.activity.HomeActivity
import com.demo.project.ui.activity.LoginActivity
import com.demo.project.ui.activity.SplashActivity
import com.demo.project.ui.activity.VideoPlayerActivity
import com.demo.project.ui.activity.WebActivity
import com.demo.project.ui.activity.WebExplorerActivity
import java.lang.reflect.Method

object SchemeUtils {

    private const val TAG = "SchemeUtils"


    const val KEY_SPLASH_ACTIVITY = "splash"
    const val KEY_HOME_ACTIVITY = "home"
    const val KEY_LOGIN_ACTIVITY = "login"
    const val KEY_WEB_ACTIVITY = "web"
    const val KEY_BROWSER_ACTIVITY = "browser"
    const val KEY_CAMERA_ACTIVITY = "camera"
    const val KEY_VIDEO_ACTIVITY = "video"
    const val KEY_DEBUG_ACTIVITY = "debug"

    fun toOpenActivity(mContext: Context, urlString: Uri) {
        when (urlString.host) {
            KEY_SPLASH_ACTIVITY -> SplashActivity.start(mContext)
            KEY_HOME_ACTIVITY -> HomeActivity.start(mContext)
            KEY_LOGIN_ACTIVITY -> LoginActivity.start(mContext)
            KEY_WEB_ACTIVITY -> {
                val linkUrl = urlString.getQueryParameter("link_url")
                val titleString = urlString.getQueryParameter("title")
                linkUrl?.let { WebActivity.start(mContext, it, titleString) }
            }

            KEY_BROWSER_ACTIVITY -> {
                val linkUrl = urlString.getQueryParameter("link_url")
                linkUrl?.let { WebExplorerActivity.start(mContext, it) }
            }

            KEY_CAMERA_ACTIVITY -> {
                CameraActivity.start(mContext)
            }

            KEY_VIDEO_ACTIVITY -> {
                val urlString = urlString.getQueryParameter("url")
                VideoPlayerActivity.start(mContext, urlString)
            }

            KEY_DEBUG_ACTIVITY -> DebugActivity.start(mContext)
            else -> LogUtil.w(TAG, "未匹配到 host: ${urlString.host}")
        }
    }

}
