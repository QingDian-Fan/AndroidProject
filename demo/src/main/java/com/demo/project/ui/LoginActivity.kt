package com.demo.project.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
        fun start(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)
            if (context !is android.app.Activity) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup): ActivityLoginBinding =
        ActivityLoginBinding.inflate(inflater, container, false)

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