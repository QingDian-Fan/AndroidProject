package com.dian.demo.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityLoginBinding
import com.dian.annotation.LoginActivity


@LoginActivity
class LoginActivity : BaseAppBindActivity<ActivityLoginBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, LoginActivity::class.java)
            if (mContext !is Activity){
                intent.flags = FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_login

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().setOpenStatusBar(false)
        getTitleBarView().visibility = gone
    }
}