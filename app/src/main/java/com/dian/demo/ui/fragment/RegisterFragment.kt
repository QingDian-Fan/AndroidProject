package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentRegisterBinding
import com.dian.demo.di.vm.LoginViewModel
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.singleClick


class RegisterFragment : BaseAppVMFragment<FragmentRegisterBinding,LoginViewModel>() {


    override fun createViewModel(): LoginViewModel = LoginViewModel()

    override fun getLayoutId(): Int = R.layout.fragment_register

    override fun initialize(savedInstanceState: Bundle?) {
        with(binding){
            btnRegister.singleClick {
               val userName = etUsername.text.toString().trim()
               val password =  etPassword.text.toString().trim()
                viewModel.doRegister(userName,password,password)
            }
        }
        viewModel.loginInfo.observeNonNull(this){
            showToast("成功")
        }
    }
}