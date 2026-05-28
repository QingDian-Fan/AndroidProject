package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentCoinRecordBinding
import com.dian.demo.di.vm.CoinRecordViewModel
import com.dian.demo.ui.adapter.CoinRecordAdapter
import com.project.common.utils.CustomDividerItemDecoration
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.project.common.utils.ext.observeNonNull


class CoinRecordFragment : BaseAppVMFragment<FragmentCoinRecordBinding, CoinRecordViewModel>() {

    private var page = 0
    private var mAdapter: CoinRecordAdapter?=null

    override fun getViewModelClass(): Class<CoinRecordViewModel> = CoinRecordViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_coin_record

    override fun initialize(savedInstanceState: Bundle?) {

        requireActivity().supportFragmentManager.setFragmentResultListener("KEY_COIN_LIST_PAGE", viewLifecycleOwner) { key, bundle ->
            val coinCount = bundle.getString("coinCount")
            binding.tvCoin.text = coinCount
        }

        SmartRefreshUtil.with(binding.layoutRefresh).autoRefresh()
        SmartRefreshUtil.with(binding.layoutRefresh).setRefreshListener {
            page = 0
            viewModel.getCoinRecordList(page = page)
            binding.layoutRefresh.setEnableRefresh(false)

        }
        SmartRefreshUtil.with(binding.layoutRefresh).setLoadMoreListener {
            page = page + 1
            viewModel.getCoinRecordList(page)
        }
        viewModel.coinRecordData.observeNonNull(this) {
            binding.layoutRefresh.finishRefresh()
            binding.layoutRefresh.finishLoadMore()

            if (mAdapter == null) {
                mAdapter = CoinRecordAdapter()
                binding.rvData.layoutManager = LinearLayoutManager(requireContext())
                binding.rvData.addItemDecoration(
                    CustomDividerItemDecoration(
                        2,
                        ResourcesUtil.getColor(R.color.line_color)
                    )
                )
                binding.rvData.adapter = mAdapter

            }

            if (page == 0) {
                mAdapter?.submitList(it.toMutableList())
            } else {
                val mergeList = mAdapter?.currentList?.toMutableList()
                mergeList?.addAll(it)
                mAdapter?.submitList(mergeList)
            }
        }
    }

    companion object {
        @JvmStatic
        fun getFragment() = CoinRecordFragment()
    }
}