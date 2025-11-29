package com.dian.demo.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.graphics.toColorInt
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.demo.project.utils.ext.gone
import com.dian.annotation.LoginPage
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityLoginBinding
import com.dian.demo.ui.titlebar.CommonTitleBar
import com.dian.demo.ui.titlebar.ScreenUtils
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.ext.gone
import com.dian.demo.utils.ext.visible


@LoginPage
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
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment


        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment) {
                changeTitleBar(true)
            }else{
                changeTitleBar(false)
            }
        }

        getTitleBarView().setListener { v, action, extra ->
            if (action == CommonTitleBar.ACTION_RIGHT_TEXT) {
                Navigation.findNavController(binding.fragmentContainerView).navigate(R.id.registerFragment)
            }else  if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressed()
            }
        }
    }

    fun changeTitleBar(isLoginPage: Boolean){
        LogUtil.e("TAG--->","isLoginPage::${isLoginPage}")
        if (isLoginPage){
            setPageTitle("登录")
            getTitleBarView().setRightText("注册", "#FF40a9ff".toColorInt(), ScreenUtils.dp2PxInt(this, 16f))
            getTitleBarView().rightTextView.visible()
        }else{
            setPageTitle("注册")
            getTitleBarView().rightTextView.gone()
        }
    }
}