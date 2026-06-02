package com.demo.project.ui.fragment

import android.os.Bundle
import com.common.ui.BaseAppVMFragment
import com.common.utils.ext.observeNonNull
import com.common.utils.ext.singleClick
import com.demo.project.R
import com.demo.project.databinding.FragmentLoginBinding
import com.demo.project.vm.LoginViewModel

class LoginFragment : BaseAppVMFragment<FragmentLoginBinding, LoginViewModel>() {

    override fun getViewModelClass(): Class<LoginViewModel> = LoginViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_login

    override fun initialize(savedInstanceState: Bundle?) {
        with(binding) {
            btnLoginCommit.singleClick {
                val userName = etLoginPhone.text.toString().trim()
                val password = etLoginPassword.text.toString().trim()
                viewModel.doLogin(userName, password)
            }
        }

        viewModel.loginInfo.observeNonNull(viewLifecycleOwner) {
            showToast("登录成功：$it")
            activity?.finish()
        }
    }
}