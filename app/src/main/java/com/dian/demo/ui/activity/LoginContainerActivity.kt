package com.dian.demo.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dian.annotation.LoginPage
import com.dian.annotation.RequireLogin
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityLoginBinding

@RequireLogin
class LoginContainerActivity : BaseAppBindActivity<ActivityLoginBinding>() {

    override fun getLayoutId(): Int = R.layout.activity_login_container

    override fun initialize(savedInstanceState: Bundle?) {

    }
}