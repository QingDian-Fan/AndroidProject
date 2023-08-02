package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.layout_visibility
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityLoginBinding

class LoginActivity : BaseAppBindActivity<ActivityLoginBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, LoginActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_login

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().setOpenStatusBar(false)
        getTitleBarView().visibility = gone
    }
}