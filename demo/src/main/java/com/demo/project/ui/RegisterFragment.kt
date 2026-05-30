package com.demo.project.ui

import android.os.Bundle
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.common.ui.BaseAppVMFragment
import com.common.utils.ext.observeNonNull
import com.common.utils.ext.singleClick
import com.demo.project.R
import com.demo.project.databinding.FragmentRegisterBinding
import com.demo.project.vm.LoginViewModel

class RegisterFragment : BaseAppVMFragment<FragmentRegisterBinding, LoginViewModel>() {

    override fun getViewModelClass(): Class<LoginViewModel> = LoginViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_register

    override fun initialize(savedInstanceState: Bundle?) {
        with(binding) {
            btnRegisterCommit.singleClick {
                val userName = etRegisterPhone.text.toString().trim()
                val password = etRegisterPassword.text.toString().trim()
                viewModel.doRegister(userName, password, password)
            }
        }

        viewModel.loginInfo.observeNonNull(viewLifecycleOwner) {
            showToast("注册成功")
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.loginFragment, false)
                .build()
            Navigation.findNavController(binding.btnRegisterCommit)
                .navigate(R.id.loginFragment, null, options)
        }
    }
}