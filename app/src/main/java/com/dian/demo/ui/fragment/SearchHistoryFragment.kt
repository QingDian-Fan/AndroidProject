package com.dian.demo.ui.fragment

import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSearchHistoryBinding
import com.dian.demo.di.vm.SearchViewModel
import com.dian.demo.ui.adapter.SearchRecordAdapter
import com.project.common.utils.ext.gone
import com.project.common.utils.ext.observeNonNull
import com.project.common.utils.ext.visible

class SearchHistoryFragment : BaseAppVMFragment<FragmentSearchHistoryBinding, SearchViewModel>() {
     var mLocalAdapter:SearchRecordAdapter? = null

    override fun isUseActivityViewModel():Boolean = true

    override fun getViewModelClass(): Class<SearchViewModel> = SearchViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_search_history

    override fun initialize(savedInstanceState: Bundle?) {
        initData()
    }

    private fun initData() {
        viewModel.getSearchHotHistoryRecord()
        viewModel.mHotSearchList.observeNonNull(this) {
            val searchRecordAdapter = SearchRecordAdapter(requireActivity(), it)
            searchRecordAdapter.setClickListener {
                viewModel.clickData.value = it
            }
            binding.flHot.setAdapter(searchRecordAdapter)
        }
        binding.tvHistory.gone()
        viewModel.getHistoryRecord()
        viewModel.mLocalSearchList.observeNonNull(requireActivity()) {
            if (it.isEmpty()) {
                binding.tvHistory.gone()
            } else {
                binding.tvHistory.visible()
                if (mLocalAdapter==null){
                    mLocalAdapter = SearchRecordAdapter(requireActivity(), it)
                    mLocalAdapter?.setClickListener {
                        viewModel.clickData.value = it
                    }
                    binding.flHistory.setAdapter(mLocalAdapter)
                }else{
                    mLocalAdapter?.notifyDataSetChanged()
                }

            }
        }

    }


    companion object {
        @JvmStatic
        fun getFragment() = SearchHistoryFragment()
    }
}