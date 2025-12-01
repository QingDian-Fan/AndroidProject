package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.dian.demo.ProjectApplication
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.constant.ANDROID_ASSET_URI
import com.dian.demo.databinding.FragmentSettingBinding
import com.dian.demo.di.vm.SettingViewModel
import com.dian.demo.ui.activity.DebugActivity
import com.dian.demo.ui.activity.H5ContainerActivity
import com.dian.demo.ui.activity.LoginActivity
import com.dian.demo.utils.CacheUtil
import com.dian.demo.utils.LoginUtil
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.singleClick


class SettingFragment : BaseAppVMFragment<FragmentSettingBinding, SettingViewModel>() {


    override fun getLayoutId(): Int = R.layout.fragment_setting

    override fun initialize(savedInstanceState: Bundle?) {
        setPageTitle("设置")
        SmartRefreshUtil.with(binding.smartLayout).setScrollMode()
        binding.llCache.setRightText(CacheUtil.getTotalCacheSize(
            ProjectApplication.getAppContext()
        ))

        binding.llCache.setOnClickListener {
            CacheUtil.clearAllCache(requireActivity())
            binding.llCache.postDelayed({
                binding.llCache.setRightText(CacheUtil.getTotalCacheSize(
                    ProjectApplication.getAppContext()
                ))
            }, 500)
        }
        binding.llAbout.setOnClickListener {
            Navigation.findNavController(binding.llAbout).navigate(R.id.aboutFragment)
        }
        binding.llPrivacy.setOnClickListener {
            H5ContainerActivity.start(requireActivity(),"${ANDROID_ASSET_URI}privacy_policy.html")
        }
        binding.llDebug.singleClick {
            DebugActivity.start(requireActivity())
        }

        binding.btnLoginout.singleClick {
            viewModel.doLoginOut()
        }

        viewModel.isLoginOut.observeNonNull(this) {
            ToastUtil.showToast(str = if (it) "退出成功" else "退出失败")
            if (it) {
                LoginUtil.clearLoginInfo()
                LoginActivity.start(requireActivity())
                activity?.finish()
            }
        }
    }

    override fun getViewModelClass(): Class<SettingViewModel> = SettingViewModel::class.java


}