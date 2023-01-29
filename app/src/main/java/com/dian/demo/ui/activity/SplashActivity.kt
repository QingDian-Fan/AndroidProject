package com.dian.demo.ui.activity

import android.Manifest
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.lifecycle.Observer
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivitySplashBinding
import com.dian.demo.utils.ShortCutUtil
import com.dian.demo.utils.permissions.LivePermissions
import com.dian.demo.utils.permissions.PermissionResult

class SplashActivity : BaseAppBindActivity<ActivitySplashBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_splash

    /**
     *  初始化操作
     */
    override fun initialize(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) initShortCut()

        getTitleBarView().visibility = gone

        LivePermissions(this@SplashActivity)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
            .observe(this, Observer {
                when (it) {
                    is PermissionResult.Grant -> {  //权限允许
                        getTitleBarView().postDelayed({
                            DemoActivity.start(this@SplashActivity)
                            finish()
                        }, 1500)
                    }
                    is PermissionResult.Rationale -> {  //权限拒绝
                        showToast("权限拒绝")
                        getTitleBarView().postDelayed({
                            DemoActivity.start(this@SplashActivity)
                            finish()
                        }, 1500)
                    }
                    is PermissionResult.Deny -> {   //权限拒绝，且勾选了不再询问
                        showToast("权限拒绝，且勾选了不再询问")
                        getTitleBarView().postDelayed({
                            DemoActivity.start(this@SplashActivity)
                            finish()
                        }, 1500)
                    }
                }
            })


    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun initShortCut() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        shortcutManager.dynamicShortcuts = ShortCutUtil().createShortCut(this@SplashActivity)
    }
}