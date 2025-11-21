package com.dian.demo.skin

import android.app.Application
import android.content.res.Configuration
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.mode.UIModeManager
import skin.support.SkinCompatManager
import skin.support.app.SkinAppCompatViewInflater
import skin.support.app.SkinCardViewInflater
import skin.support.constraint.app.SkinConstraintViewInflater
import skin.support.design.app.SkinMaterialViewInflater


open class SkinApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        SkinCompatManager.withoutActivity(this)
            .addInflater(SkinAppCompatViewInflater()) // 基础控件换肤初始化
         //   .addInflater(SkinMaterialViewInflater()) // material design 控件换肤初始化[可选]
            .addInflater(SkinConstraintViewInflater()) // ConstraintLayout 控件换肤初始化[可选]
            .addInflater(SkinCardViewInflater()) // CardView v7 控件换肤初始化[可选]
            .setSkinStatusBarColorEnable(false) // 关闭状态栏换肤，默认打开[可选]
            .setSkinWindowBackgroundEnable(false) // 关闭windowBackground换肤，默认打开[可选]
            .loadSkin()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val currentNightMode = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            Configuration.UI_MODE_NIGHT_NO -> {
                LogUtil.e("onConfigurationChanged: uiMode=白天模式")
                UIModeManager.getInstance().broadCastUiModeChanged(false)
            }
            Configuration.UI_MODE_NIGHT_YES -> {
                LogUtil.e("onConfigurationChanged: uiMode=黑夜模式")
                UIModeManager.getInstance().broadCastUiModeChanged(true)
            }
        }
    }
}