package com.dian.demo.ui.fragment

import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentMineBinding
import com.dian.demo.di.vm.SettingViewModel
import com.dian.demo.ui.activity.ContainerActivity
import com.dian.demo.ui.activity.DemoActivity
import com.dian.demo.ui.activity.LoginActivity
import com.dian.demo.ui.activity.LoginContainerActivity
import com.dian.demo.ui.activity.TodoListActivity
import com.dian.demo.utils.LoginUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ext.gone
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.singleClick
import com.dian.demo.utils.ext.visible


class MineFragment : BaseAppVMFragment<FragmentMineBinding, SettingViewModel>() {

    companion object {
        @JvmStatic
        fun getFragment() = MineFragment()
    }

    override fun getLayoutId(): Int = R.layout.fragment_mine

    override fun createViewModel(): SettingViewModel = SettingViewModel()

    override fun initialize(savedInstanceState: Bundle?) {

        SmartRefreshUtil.with(binding.smartLayout).setScrollMode()

        binding.llCollect.singleClick {
            LoginContainerActivity.start(requireActivity(),1)
        }
        binding.llShare.singleClick {
            LoginContainerActivity.start(requireActivity(),0)
        }
        binding.sbTodo.singleClick {
            TodoListActivity.start(requireActivity())
        }
        binding.llBrowserCollect.setOnClickListener {
            ContainerActivity.start(requireActivity(), true,2)
        }
        binding.llReadLater.setOnClickListener {
            ContainerActivity.start(requireActivity(), true,1)
        }
        binding.llReadRecord.singleClick {
            ContainerActivity.start(requireActivity(), true,0)
        }
        binding.sbMenu.singleClick {
            DemoActivity.start(requireActivity())
        }
        binding.llSetting.setOnClickListener {
            ContainerActivity.start(requireActivity(),false,0)
        }

        binding.ivUserLogo.setOnClickListener {
            if (!LoginUtil.isLogin()) {
                LoginActivity.start(requireActivity())
            }
        }
        initData()
    }

    override fun onResume() {
        super.onResume()
        updateUserData()
    }
    private fun initData() {
        updateUserData()


    }

    fun updateUserData(){
        updateUserUI()
        if (LoginUtil.isLogin() && LoginUtil.getLoginInfo() == null) {
            viewModel.getUserInfo()
        } else {
            binding.tvUserDesc.gone()
        }
        viewModel.userData.observeNonNull(this) {
            LoginUtil.saveLoginInfo(it)
            updateUserUI()
        }
    }

    private fun updateUserUI(){
        LoginUtil.getLoginInfo()?.let {
            binding.tvUserDesc.visible()
            binding.tvUserName.text = it.coinInfo?.username
            binding.tvUserDesc.text = "等级：${it.coinInfo?.level}  排名：${it.coinInfo?.rank}"
        }
        LoginUtil.getLoginInfo()?:run{
            binding.tvUserDesc.gone()
            binding.tvUserName.text = "未登录"
        }
    }


}