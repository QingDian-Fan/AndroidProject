package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Color
import android.os.Bundle
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment
import com.common.ui.skin.BaseSkinBindActivity
import com.common.utils.ext.gone
import com.common.utils.ext.visible
import com.common.weight.titlebar.CommonTitleBar
import com.common.weight.titlebar.ScreenUtils
import com.demo.project.R
import com.demo.project.databinding.ActivityLoginBinding

class LoginActivity : BaseSkinBindActivity<ActivityLoginBinding>() {

    companion object {
        @JvmStatic
        fun start(mContext: Context) {
            val intent = Intent(mContext, LoginActivity::class.java).apply {
                if (mContext !is Activity) {
                    flags=FLAG_ACTIVITY_NEW_TASK
                }
            }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_login

    override fun initialize(savedInstanceState: Bundle?) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment

        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            changeTitleBar(destination.id == R.id.loginFragment)
        }

        getTitleBarView()?.setListener { _, action, _ ->
            when (action) {
                CommonTitleBar.ACTION_LEFT_BUTTON -> onBackPressedDispatcher.onBackPressed()
                CommonTitleBar.ACTION_RIGHT_TEXT ->
                    Navigation.findNavController(binding.fragmentContainerView)
                        .navigate(R.id.registerFragment)
            }
        }
    }

    private fun changeTitleBar(isLoginPage: Boolean) {
        if (isLoginPage) {
            setPageTitle("登录")
            getTitleBarView()?.setRightText("注册", Color.parseColor("#FF40A9FF"), ScreenUtils.dp2PxInt(this, 16f))
            getTitleBarView()?.getRightTextView()?.visible()
        } else {
            setPageTitle("注册")
            getTitleBarView()?.getRightTextView()?.gone()
        }
    }
}