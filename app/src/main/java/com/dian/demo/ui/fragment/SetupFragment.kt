package com.dian.demo.ui.fragment

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSetupBinding
import com.dian.demo.di.vm.SetupViewModel
import com.dian.demo.ui.adapter.HomePagerAdapter
import com.dian.demo.ui.adapter.SetUpCommonNavigatorAdapter
import com.dian.demo.utils.ext.dpToPx
import net.lucode.hackware.magicindicator.MagicIndicator
import net.lucode.hackware.magicindicator.ViewPagerHelper
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView


class SetupFragment : BaseAppVMFragment<FragmentSetupBinding, SetupViewModel>() {

    private val titles = mutableListOf<String>("推荐", "视频")
    private val fragmentList by lazy {
        listOf(
            SetUpChildFragment.getFragment(),
            SetUpChildFragment.getFragment()
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