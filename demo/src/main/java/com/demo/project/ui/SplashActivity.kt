package com.demo.project.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.common.ui.BaseAppBindActivity
import com.common.utils.ext.gone
import com.demo.project.databinding.ActivitySplashBinding

class SplashActivity : BaseAppBindActivity<ActivitySplashBinding>() {

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup): ActivitySplashBinding =
        ActivitySplashBinding.inflate(inflater, container, false)

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.gone()
        binding.root.postDelayed({
            VideoPlayerActivity.start(this@SplashActivity)
            finish()
        }, 2500)
    }
}