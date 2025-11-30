package com.dian.demo.ui.fragment

import android.content.Intent
import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.constant.HOOK_AMS_EXTRA_NAME
import com.dian.demo.databinding.FragmentLoginBinding
import com.dian.demo.di.vm.LoginViewModel
import com.dian.demo.ui.activity.LoginActivity
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.singleClick
import com.dian.demo.utils.LoginUtil


/**
 * companion object {
 *    @JvmStatic
 *    fun getFragment() = LoginFragment().apply {
 *            arguments = Bundle().apply {
 *               putString(ARG_PARAM1, param1)
 *            }
 *        }
 * }
 *
 */

class LoginFragment : BaseAppVMFragment<FragmentLoginBinding, LoginViewModel>() {


    override fun createViewModel(): LoginViewModel = LoginViewModel()

    override fun getLayoutId(): Int = R.layout.fragment_login

    override fun initialize(savedInstanceState: Bundle?) {


        with(binding) {
            btnLoginCommit.singleClick {
                val userName = etLoginPhone.text.toString().trim()
                val password = etLoginPassword.text.toString().trim()
                viewModel.doLogin(userName, password)
            }

        }

        viewModel.loginInfo.observeNonNull(this){
            ToastUtil.showToast(str = "登录成功")
            val targetIntent = (activity as? LoginActivity)?.intent?.getParcelableExtra<Intent>(HOOK_AMS_EXTRA_NAME)
            if (targetIntent != null) {
                startActivity(targetIntent)
                activity?.finish()
            }else{
               activity?.finish()
            }
        }
    }
}