package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSetUpChildBinding
import com.dian.demo.di.model.NavigationData
import com.dian.demo.di.model.SetUpData
import com.dian.demo.di.vm.SetupViewModel
import com.dian.demo.ui.activity.ContainerActivity
import com.dian.demo.ui.adapter.SetupAdapter
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ext.observeNonNull


class SetUpChildFragment : BaseAppVMFragment<FragmentSetUpChildBinding, SetupViewModel>() {

    override fun getLayoutId(): Int = R.layout.fragment_set_up_child

    override fun isUseActivityViewModel(): Boolean = true

    override fun getViewModelClass(): Class<SetupViewModel> = SetupViewModel::class.java

    override fun initialize(savedInstanceState: Bundle?) {

    }

    override fun lazyInit() {
        super.lazyInit()
        val page = arguments?.getInt("page") ?: 0
        SmartRefreshUtil.with(binding.layoutRefresh).setScrollMode()
        if (page == 0) {
            viewModel.getSetUpDataList()
        } else {
            viewModel.getNavigationData()
        }

        viewModel.mSetUpData.observeNonNull(this) {
            if (page == 0) {
                initAdapter(page, it)
            }
        }

        viewModel.mNavigationData.observeNonNull(this) {
            if (page == 1) {
                initAdapter(page, it)
            }
        }
    }

    var setupAdapter: SetupAdapter? = null
    private fun initAdapter(page: Int, dataList: MutableList<SetUpData>) {
        if (setupAdapter == null) {
            binding.rvData.layoutManager = LinearLayoutManager(requireContext())
            setupAdapter = SetupAdapter(page == 0)
            setupAdapter?.setListener { isSetup, titleList, data ->
                ContainerActivity.start(requireActivity(),0,titleList,data)
            }
            binding.rvData.adapter = setupAdapter
        }
        setupAdapter?.submitList(dataList)

    }


    companion object {

        @JvmStatic
        fun getFragment(page: Int) = SetUpChildFragment().apply {
            arguments = bundleOf("page" to page)
        }
    }
}