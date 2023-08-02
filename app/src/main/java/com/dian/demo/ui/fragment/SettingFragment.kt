package com.dian.demo.ui.fragment

import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSettingBinding
import com.dian.demo.di.vm.SettingViewModel
import com.dian.demo.ui.activity.DemoActivity
import com.dian.demo.ui.activity.LoginActivity
import com.dian.demo.ui.activity.TodoListActivity
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.singleClick
import com.tencent.bugly.crashreport.CrashReport


class SettingFragment : BaseAppVMFragment<FragmentSettingBinding, SettingViewModel>() {

    companion object {
        @JvmStatic
        fun getFragment() = SettingFragment()
    }

    override fun getLayoutId(): Int = R.layout.fragment_setting

    override fun createViewModel(): SettingViewModel = SettingViewModel()

    override fun initialize(savedInstanceState: Bundle?) {
        binding.sbLogin.singleClick {
            LoginActivity.start(requireActivity())
        }
        binding.sbTodo.singleClick {
            TodoListActivity.start(requireActivity())
        }
        binding.sbMenu.singleClick {
            DemoActivity.start(requireActivity())
        }
        binding.btnLoginout.singleClick {
            viewModel.doLoginOut()
            CrashReport.testJavaCrash();
        }
        viewModel.isLoginOut.observeNonNull(this) {
            ToastUtil.showToast(str = if (it) "退出成功" else "退出失败")
        }

    }




}