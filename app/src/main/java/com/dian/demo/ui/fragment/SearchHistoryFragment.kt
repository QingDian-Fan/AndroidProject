package com.dian.demo.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSearchHistoryBinding
import com.dian.demo.di.vm.SearchViewModel
import com.dian.demo.ui.adapter.SearchRecordAdapter
import com.dian.demo.utils.SearchRecordUtil
import com.dian.demo.utils.ext.gone
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.visible

class SearchHistoryFragment : BaseAppVMFragment<FragmentSearchHistoryBinding, SearchViewModel>() {
     var mLocalAdapter:SearchRecordAdapter? = null

    override fun isUserActivityViewModel():Boolean = true

    override fun createViewModel(): SearchViewModel = SearchViewModel()

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