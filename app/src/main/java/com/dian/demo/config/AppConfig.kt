package com.dian.demo.config


import com.demo.project.config.Constant.DEBUG_URL_CONFIG
import com.dian.demo.BuildConfig
import com.dian.demo.R
import com.dian.demo.utils.LogUtils
import com.dian.demo.utils.PreferenceUtils
import com.dian.demo.utils.ResourcesUtils


object AppConfig {




    const val QQ_APPID ="101828096"
    const val QQ_APPKEY = "9dfd3300c3aa3c4596a07796c64914b2"

    const val WX_APPID = "wxd35706cc9f46114c"
    const val WX_APPKEY ="0c8c7cf831dd135a32b3e395ea459b5a"

    const val WB_APP_KEY = "2045436852"
    const val WB_REDIRECT_URl = "http://www.sina.com"
    const val WB_SCOPE = ""




    val BASE_URL: String = ResourcesUtils.getString(R.string.release_base_url)

    fun getBaseUrl(): String{
        return if (BuildConfig.DEBUG) {
            LogUtils.e("------>", PreferenceUtils.getString(DEBUG_URL_CONFIG, BASE_URL))
            PreferenceUtils.getString(DEBUG_URL_CONFIG, BASE_URL)
        } else BASE_URL

    }
}