package com.dian.demo.ui.activity

import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivitySplashBinding
import com.dian.demo.utils.ShortCutUtil
import java.util.Timer
import java.util.TimerTask

class SplashActivity : BaseAppBindActivity<ActivitySplashBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_splash

    /**
     *  初始化操作
     */
    override fun initialize(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.N_MR1) initShortCut()

        getTitleBarView().visibility = gone
        getTitleBarView().postDelayed({
            DebugActivity.start(this@SplashActivity)
            finish()
        },3000)
      /*  val timer =Timer()
        val timerTask =object : TimerTask() {
            override fun run() {
                DebugActivity.start(this@SplashActivity)
                finish()
            }
        }
        timer.schedule(timerTask,3000)*/

    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun initShortCut(){
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        shortcutManager.dynamicShortcuts = ShortCutUtil().createShortCut(this@SplashActivity)
    }
}