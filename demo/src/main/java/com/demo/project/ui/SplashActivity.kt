package com.demo.project.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.common.ui.BaseAppBindActivity
import com.demo.project.R
import com.demo.project.databinding.ActivitySplashBinding

class SplashActivity : BaseAppBindActivity<ActivitySplashBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_splash

    override fun initialize(savedInstanceState: Bundle?) {
        binding.root.postDelayed({
            HomeActivity.start(this@SplashActivity)
            finish()
        },3000)
    }
}