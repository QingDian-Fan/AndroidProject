package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentCoinRankBinding
import com.dian.demo.di.vm.CoinRankViewModel
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.ui.adapter.CoinRankAdapter
import com.dian.demo.ui.adapter.GlobalArticleAdapter
import com.dian.demo.utils.CustomDividerItemDecoration
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ext.observeNonNull
import com.stx.xhb.androidx.transformers.Transformer


class CoinRankFragment : BaseAppVMFragment<FragmentCoinRankBinding, CoinRankViewModel>() {
    private var page = 1
    private var mAdapter: CoinRankAdapter? = null

    override fun getViewModelClass(): Class<CoinRankViewModel> = CoinRankViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_coin_rank

    override fun initialize(savedInstanceState: Bundle?) {
        SmartRefreshUtil.with(binding.layoutRefresh).autoRefresh()
        SmartRefreshUtil.with(binding.layoutRefresh).setRefreshListener {
            page = 1
            viewModel.getCoinRankList( page)
        }
        SmartRefreshUtil.with(binding.layoutRefresh).setLoadMoreListener {
            page = page + 1
            viewModel.getCoinRankList(page)
        }

        viewModel.coinRankData.observeNonNull(this) {
            binding.layoutRefresh.finishRefresh()
            binding.layoutRefresh.finishLoadMore()

            if (mAdapter == null) {
                mAdapter = CoinRankAdapter()
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
        fun getFragment() = CoinRankFragment()
    }
}