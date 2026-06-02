package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import com.common.ui.BaseAppBindActivity
import com.common.utils.ext.gone
import com.demo.project.R
import com.demo.project.databinding.ActivitySplashBinding

class SplashActivity : BaseAppBindActivity<ActivitySplashBinding>() {
    companion object {
        @JvmStatic
        fun start(mContext: Context) {
            val intent = Intent(mContext, SplashActivity::class.java).apply {
                if (mContext !is Activity) {
                    flags=FLAG_ACTIVITY_NEW_TASK
                }
            }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_splash

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.gone()
        binding.root.postDelayed({
            HomeActivity.start(this@SplashActivity)
            finish()
        }, 2500)
    }
}