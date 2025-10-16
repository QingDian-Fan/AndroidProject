package com.dian.demo.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.ShortcutManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Contacts
import android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat.startActivity
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivitySplashBinding
import com.dian.demo.utils.ShortCutUtil
import com.dian.demo.utils.aop.CheckPermissions
import com.dian.demo.utils.permissions.PermissionsUtil
import com.dian.demo.BuildConfig


class SplashActivity : BaseAppBindActivity<ActivitySplashBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_splash

    /**
     *  初始化操作
     */
    override fun initialize(savedInstanceState: Bundle?) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) initShortCut()

        getTitleBarView().visibility = gone
        if (PermissionsUtil.hasPermission(
                this@SplashActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
        ) {
            getTitleBarView().postDelayed({
                HomeActivity.start(this@SplashActivity)
                finish()
            }, 500)
            return
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            startActivity(Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
        }
        toPage()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun initShortCut() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        shortcutManager.dynamicShortcuts = ShortCutUtil().createShortCut(this@SplashActivity)
    }

    @CheckPermissions(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        isMust = false
    )
    fun toPage() {
        getTitleBarView().postDelayed({
            HomeActivity.start(this@SplashActivity)
            finish()
        }, 500)
    }
}