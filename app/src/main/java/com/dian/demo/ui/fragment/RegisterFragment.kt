package com.dian.demo.ui.fragment

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentRegisterBinding
import com.dian.demo.di.vm.LoginViewModel
import com.dian.demo.ui.activity.LoginActivity
import com.dian.demo.utils.ext.gone
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.singleClick


class RegisterFragment : BaseAppVMFragment<FragmentRegisterBinding,LoginViewModel>() {



    override fun getLayoutId(): Int = R.layout.fragment_register

    override fun getViewModelClass(): Class<LoginViewModel> = LoginViewModel::class.java

    override fun initialize(savedInstanceState: Bundle?) {

        with(binding){
            btnRegisterCommit.singleClick {
               val userName = etRegisterPhone.text.toString().trim()
               val password =  etRegisterPassword.text.toString().trim()
                viewModel.doRegister(userName,password,password)
            }
        }
        viewModel.loginInfo.observeNonNull(this){
            showToast("成功")
            val options = NavOptions.Builder()
                .setLaunchSingleTop(true)               // 避免重复创建
                .setPopUpTo(R.id.loginFragment, false)       // 栈中已存在 → 回到它并清掉上面的 fragment
                .build()
            Navigation.findNavController(binding.btnRegisterCommit).navigate(
                R.id.loginFragment,
                null,
                options
            )
        }
    }



}