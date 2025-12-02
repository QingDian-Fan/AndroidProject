package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentKnowledgeChildDetailBinding
import com.dian.demo.databinding.FragmentKnowledgeDetailBinding
import com.dian.demo.di.model.NavigationData
import com.dian.demo.di.vm.KnowledgeChildDetailViewModel
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.ui.adapter.GlobalArticleAdapter
import com.dian.demo.utils.CustomDividerItemDecoration
import com.dian.demo.utils.MoshiUtil
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ext.observeNonNull


class KnowledgeChildDetailFragment :
    BaseAppVMFragment<FragmentKnowledgeChildDetailBinding, KnowledgeChildDetailViewModel>() {

    private var page = 0
    private var mAdapter: GlobalArticleAdapter? = null
    override fun getViewModelClass(): Class<KnowledgeChildDetailViewModel> =
        KnowledgeChildDetailViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_knowledge_child_detail

    var data: NavigationData? = null
    override fun initialize(savedInstanceState: Bundle?) {
        val dataString = arguments?.getString("dataString")
        dataString?.let {
            data = MoshiUtil.fromJson<NavigationData>(it)
        }

    }

    override fun lazyInit() {
        super.lazyInit()
        SmartRefreshUtil.with(binding.layoutRefresh).autoRefresh()
        SmartRefreshUtil.with(binding.layoutRefresh).setRefreshListener {
            page = 0
            viewModel.getKnowledgeDetailData( page,data?.id?:"")
        }
        SmartRefreshUtil.with(binding.layoutRefresh).setLoadMoreListener {
            page = page + 1
            viewModel.getKnowledgeDetailData(page,data?.id?:"")
        }

        viewModel.articleData.observeNonNull(this) {
            binding.layoutRefresh.finishRefresh()
            binding.layoutRefresh.finishLoadMore()

            if (mAdapter == null) {
                mAdapter = GlobalArticleAdapter()
                binding.rvData.layoutManager = LinearLayoutManager(requireContext())
                binding.rvData.addItemDecoration(
                    CustomDividerItemDecoration(
                        2,
                        ResourcesUtil.getColor(R.color.line_color)
                    )
                )
                binding.rvData.adapter = mAdapter
                mAdapter?.setListener { link ->
                    link?.let { WebExplorerActivity.start(requireContext(), it, it) }
                }
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
        fun getFragment(bean: NavigationData?) = KnowledgeChildDetailFragment().apply {
            bean?.let {
                val dataString = MoshiUtil.toJson<NavigationData>(it)
                arguments = bundleOf("dataString" to dataString)
            }
        }
    }
}