package com.dian.demo.ui.fragment

import android.os.Bundle
import com.google.android.flexbox.FlexboxLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentSetUpChildBinding
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.JustifyContent


class SetUpChildFragment : BaseAppBindFragment<FragmentSetUpChildBinding>() {

    override fun getLayoutId(): Int = R.layout.fragment_set_up_child

    override fun initialize(savedInstanceState: Bundle?) {
        binding.rvData.layoutManager = FlexboxLayoutManager(requireActivity()).apply {
            flexDirection = FlexDirection.ROW      // 水平排列
            flexWrap = FlexWrap.WRAP               // 超出自动换行
            justifyContent = JustifyContent.FLEX_START // 左对齐
        }
    }

    companion object {

        @JvmStatic
        fun getFragment() = SetUpChildFragment()
    }
}