package com.common.share

import android.content.Context
import com.common.share.ShareConfig.WB_APP_KEY
import com.common.share.ShareConfig.WB_REDIRECT_URl
import com.common.share.ShareConfig.WB_SCOPE
import com.common.utils.LogUtil
import com.sina.weibo.sdk.auth.AuthInfo
import com.sina.weibo.sdk.openapi.SdkListener
import com.sina.weibo.sdk.openapi.WBAPIFactory

object WeiBoSdkUtils {
    fun initWeiBoSdk(mContext: Context) {
        val authInfo = AuthInfo(mContext, WB_APP_KEY, WB_REDIRECT_URl, WB_SCOPE)
        WBAPIFactory.createWBAPI(mContext).registerApp(mContext, authInfo, object : SdkListener {
            override fun onInitSuccess() {
                // SDK初始化成功回调，成功一次后再次初始化将不再有任何回调
                LogUtil.e("SDK--->","微博SDK注册成功")
            }

            override fun onInitFailure(e: Exception) { // SDK初始化失败回调
                LogUtil.e("SDK--->","微博SDK注册失败")

            }
        })
    }
}