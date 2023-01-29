package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityHomeBinding
import com.dian.demo.ui.dialog.DebugDialog
import com.dian.demo.utils.ResourcesUtils
import com.dian.demo.utils.ScreenShotUtils
import com.dian.demo.utils.ext.showAllowStateLoss
import com.dian.demo.utils.share.dialog.ShareDialog


class HomeActivity : BaseAppBindActivity<ActivityHomeBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, HomeActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_home

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().setCenterText(ResourcesUtils.getString(R.string.home_title_text))
        getTitleBarView().leftImageButton.visibility = gone
    }

    val mDuraction = 2000 // 两次返回键之间的时间差

    var mLastTime: Long = 0 // 最后一次按back键的时刻


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) { // 截获back事件
            exitApp()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    private fun exitApp() = if (System.currentTimeMillis() - mLastTime > mDuraction) {
        showToast(ResourcesUtils.getString(R.string.exit_app))
        mLastTime = System.currentTimeMillis()
    } else {
        onBackPressed()
    }


    fun clickView(view: View) {
        when (view.id) {
            R.id.btn_share_text -> {
                ShareDialog().setText(true, ResourcesUtils.getString(R.string.app_content))
                    .showAllowStateLoss(supportFragmentManager, "share-text")
            }
            R.id.btn_share_link -> {
                val iconLogo = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_pink)
                ShareDialog().setLinkData(
                    true,
                    iconLogo,
                    ResourcesUtils.getString(R.string.app_website),
                    ResourcesUtils.getString(R.string.app_name),
                    ResourcesUtils.getString(R.string.app_content)
                ).showAllowStateLoss(supportFragmentManager, "share-link")
            }
            R.id.btn_share_bitmap -> {
                val bitmap = ScreenShotUtils.shotActivityNoStatusBar(this)
                ShareDialog().setBitmapData(bitmap)
                    .showAllowStateLoss(supportFragmentManager, "share-bitmap")
            }
            R.id.btn_debug_activity -> {
                DebugActivity.start(this)
            }
            R.id.btn_scan_activity -> {
                ScanActivity.start(this)
            }
            R.id.btn_generate_activity -> {
                GenerateActivity.start(this)
            }
            R.id.btn_web_activity -> {
                DebugDialog().showAllowStateLoss(supportFragmentManager, "web")
            }
        }
    }
}