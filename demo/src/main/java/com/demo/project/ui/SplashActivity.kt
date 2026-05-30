package com.demo.project.ui

import android.os.Bundle
import com.common.ui.BaseAppBindActivity
import com.common.utils.ext.gone
import com.demo.project.R
import com.demo.project.databinding.ActivitySplashBinding

class SplashActivity : BaseAppBindActivity<ActivitySplashBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_splash

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.gone()
        binding.root.postDelayed({
            HomeActivity.start(this@SplashActivity)
            finish()
        }, 2500)
    }
}