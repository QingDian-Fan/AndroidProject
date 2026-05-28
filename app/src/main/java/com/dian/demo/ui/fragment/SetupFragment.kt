package com.dian.demo.ui.fragment

import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSetupBinding
import com.dian.demo.di.vm.SetupViewModel
import com.dian.demo.ui.adapter.HomePagerAdapter
import com.dian.demo.ui.adapter.SetUpCommonNavigatorAdapter
import net.lucode.hackware.magicindicator.ViewPagerHelper
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator


class SetupFragment : BaseAppVMFragment<FragmentSetupBinding, SetupViewModel>() {

    private val titles = mutableListOf<String>("体系", "导航")
    private val fragmentList by lazy {
        listOf(
            SetUpChildFragment.getFragment(0),
            SetUpChildFragment.getFragment(1)
        )
    }

    override fun getLayoutId(): Int = R.layout.fragment_setup

    override fun initialize(savedInstanceState: Bundle?) {
        initView()
    }

    private fun initView() {
        binding.vpData.adapter = HomePagerAdapter(fragmentList, childFragmentManager)
        val commonNavigator = CommonNavigator(requireActivity())
        commonNavigator.adapter = SetUpCommonNavigatorAdapter(titles)
        binding.magicIndicator.navigator = commonNavigator
        ViewPagerHelper.bind(binding.magicIndicator, binding.vpData)
    }

    override fun getViewModelClass(): Class<SetupViewModel> = SetupViewModel::class.java

    companion object {
        @JvmStatic
        fun getFragment() = SetupFragment()
    }
}