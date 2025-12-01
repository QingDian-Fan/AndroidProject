package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSearchResultBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.vm.SearchData
import com.dian.demo.di.vm.SearchViewModel
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.ui.adapter.GlobalArticleAdapter
import com.dian.demo.utils.CustomDividerItemDecoration
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ext.observeNonNull


class SearchResultFragment : BaseAppVMFragment<FragmentSearchResultBinding, SearchViewModel>() {


    private val dataList: ArrayList<ArticleBean> by lazy { arrayListOf() }
    private var page = 0
    private var mAdapter: GlobalArticleAdapter? = null


    override fun isUseActivityViewModel(): Boolean = true
    override fun getViewModelClass(): Class<SearchViewModel> = SearchViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_search_result

    override fun initialize(savedInstanceState: Bundle?) {
        initData()
    }

    private fun initData() {
        SmartRefreshUtil.with(binding.layoutRefresh).autoRefresh()
        SmartRefreshUtil.with(binding.layoutRefresh).setRefreshListener {
            page = 0
            viewModel.loadData.value?.let {
                viewModel.getSearchList(page, it.keyword)
                viewModel.loadData.value = SearchData(0, it.keyword)
            }

        }
        SmartRefreshUtil.with(binding.layoutRefresh).setLoadMoreListener {
            viewModel.loadData.value?.let {
                page = it.page + 1
                viewModel.getSearchList(page, it.keyword)
                viewModel.loadData.value = SearchData(page, it.keyword)
            }
        }
        viewModel.articleData.observeNonNull(this) {
            binding.layoutRefresh.finishRefresh()
            binding.layoutRefresh.finishLoadMore()

            if (mAdapter == null) {
                mAdapter = GlobalArticleAdapter()
                binding.rvData.layoutManager = LinearLayoutManager(requireContext())
                binding.rvData.addItemDecoration(CustomDividerItemDecoration(2, ResourcesUtil.getColor(R.color.line_color)))
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
        fun getFragment() = SearchResultFragment()
    }
}