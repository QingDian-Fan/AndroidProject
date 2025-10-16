package com.dian.demo.http.interceptor


import com.dian.demo.ProjectApplication
import com.dian.demo.R
import com.dian.demo.utils.DeviceIdUtil
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.Utils
import com.dian.demo.utils.encryption.AESUtils
import okhttp3.Interceptor
import okhttp3.Response
import com.dian.demo.BuildConfig


/**
 * 中文（中国）：values-zh-rCN
 * 中文（中国台湾）：values-zh-rTW
 * 中文（中国香港）：values-zh-rHK
 * 英语（美国）：values-en-rUS
 * 英语（英国）：values-en-rGB
 * 英文（澳大利亚）：values-en-rAU
 * 英文（加拿大）：values-en-rCA
 * 英文（爱尔兰）：values-en-rIE
 * 英文（印度）：values-en-rIN
 * 英文（新西兰）：values-en-rNZ
 * 英文（新加坡）：values-en-rSG
 * 英文（南非）：values-en-rZA
 * 阿拉伯文（埃及）：values-ar-rEG
 * 阿拉伯文（以色列）：values-ar-rIL
 * 保加利亚文: values-bg-rBG
 * 加泰罗尼亚文：values-ca-rES
 * 捷克文：values-cs-rCZ
 * 丹麦文：values-da-rDK
 * 德文（奥地利）：values-de-rAT
 * 德文（瑞士）：values-de-rCH
 * 德文（德国）：values-de-rDE
 * 德文（列支敦士登）：values-de-rLI
 * 希腊文：values-el-rGR
 * 西班牙文（西班牙）：values-es-rES
 * 西班牙文（美国）：values-es-rUS
 * 芬兰文（芬兰）：values-fi-rFI
 * 法文（比利时）：values-fr-rBE
 * 法文（加拿大）：values-fr-rCA
 * 法文（瑞士）：values-fr-rCH
 * 法文（法国）：values-fr-rFR
 * 希伯来文：values-iw-rIL
 * 印地文：values-hi-rIN
 * 克罗里亚文：values-hr-rHR
 * 匈牙利文：values-hu-rHU
 * 印度尼西亚文：values-in-rID
 * 意大利文（瑞士）：values-it-rCH
 * 意大利文（意大利）：values-it-rIT
 * 日文：values-ja-rJP
 * 韩文：values-ko-rKR
 * 立陶宛文：valueslt-rLT
 * 拉脱维亚文：values-lv-rLV
 * 挪威博克马尔文：values-nb-rNO
 * 荷兰文(比利时)：values-nl-BE
 * 荷兰文（荷兰）：values-nl-rNL
 * 波兰文：values-pl-rPL
 * 葡萄牙文（巴西）：values-pt-rBR
 * 葡萄牙文（葡萄牙）：values-pt-rPT
 * 罗马尼亚文：values-ro-rRO
 * 俄文：values-ru-rRU
 * 斯洛伐克文：values-sk-rSK
 * 斯洛文尼亚文：values-sl-rSI
 * 塞尔维亚文：values-sr-rRS
 * 瑞典文：values-sv-rSE
 * 泰文：values-th-rTH
 * 塔加洛语：values-tl-rPH
 * 土耳其文：values--r-rTR
 * 乌克兰文：values-uk-rUA
 * 越南文：values-vi-rVN
 * 缅甸语 ： values-my
 */
class ParamsInterceptor : Interceptor {
//
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val timeStamp = System.currentTimeMillis().toString()
        val modifiedUrl = originalRequest.url.newBuilder()
            .addQueryParameter("app_version", BuildConfig.VERSION_NAME)
            .addQueryParameter("language", ResourcesUtil.getString(R.string.language))
            .addQueryParameter("uuid", DeviceIdUtil.getInstance().getDeviceId(ProjectApplication.getAppContext()))
            .addQueryParameter("sdk_version", DeviceIdUtil.getInstance().deviceSDK.toString())
            .addQueryParameter("device_brand", DeviceIdUtil.getInstance().deviceBrand)
            .addQueryParameter("device_model", DeviceIdUtil.getInstance().deviceModel)
            .addQueryParameter("channel",Utils.getChannel())
            .addQueryParameter("time_stamp", timeStamp)
            .addQueryParameter("signature", AESUtils.getInstance().encrypt("${BuildConfig.APPLICATION_ID}-$timeStamp"))
            .build()
        val request = originalRequest.newBuilder().url(modifiedUrl).build()
        return chain.proceed(request)
    }
}