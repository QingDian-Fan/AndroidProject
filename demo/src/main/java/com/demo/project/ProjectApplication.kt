package com.demo.project

import com.common.http.HttpUtils
import com.common.scan.wechat.WeChatQRCodeDetector
import com.common.theme.BaseApplication
import com.common.utils.Utils
import org.opencv.OpenCV

class ProjectApplication: BaseApplication() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(this,BuildConfig.isDebug)
        HttpUtils.getInstance().init()
        OpenCV.initOpenCV()
        WeChatQRCodeDetector.init(this)
    }
}
