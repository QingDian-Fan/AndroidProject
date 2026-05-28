package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.paging.PagingData
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentAnswersBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.vm.AnswersViewModel
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.ui.adapter.GlobalPagingArticleAdapter
import com.project.common.utils.CustomDividerItemDecoration
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.project.common.utils.ext.observeNonNull


class AnswersFragment : BaseAppVMFragment<FragmentAnswersBinding, AnswersViewModel>() {

    private var page = 0
    private var mAdapter: GlobalPagingArticleAdapter? = null
    private val dataList: ArrayList<ArticleBean> by lazy { arrayListOf() }

    override fun getViewModelClass(): Class<AnswersViewModel> = AnswersViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_answers

    override fun initialize(savedInstanceState: Bundle?) {

        initData()
    }

    private fun initData() {
        SmartRefreshUtil.with(binding.layoutRefresh).autoRefresh()
        SmartRefreshUtil.with(binding.layoutRefresh).setRefreshListener {
            page = 0
            viewModel.getAnswersList(page = page)
        }
        SmartRefreshUtil.with(binding.layoutRefresh).setLoadMoreListener {
            page = page + 1
            viewModel.getAnswersList(page)
        }

        viewModel.articleData.observeNonNull(this) {
            binding.layoutRefresh.finishRefresh()
            binding.layoutRefresh.finishLoadMore()

            if (mAdapter == null) {
                mAdapter = GlobalPagingArticleAdapter()
                binding.rvData.layoutManager = LinearLayoutManager(requireContext())
                binding.rvData.addItemDecoration(
                    CustomDividerItemDecoration(
                        2,
                        ResourcesUtil.getColor(R.color.line_color)
                    )
                )
                binding.rvData.adapter = mAdapter
                mAdapter?.setCollectListener { position, isChecked, bean ->
                    bean.collect = isChecked
                    if (isChecked) {
                        viewModel.collectArticle(bean.id.toString())
                    } else {
                        viewModel.cancelCollectArticle(bean.id.toString())
                    }
                }
                mAdapter?.setListener { link ->
                    link?.let { WebExplorerActivity.start(requireContext(), it, it) }
                }
            }

            if (page == 0) {
                dataList.clear()
                dataList.addAll(it)
                mAdapter?.refresh()
                val mPagingData = PagingData.from(it)
                mAdapter?.submitData(lifecycle, mPagingData)
            } else {
                dataList.addAll(it)
                mAdapter?.submitData(lifecycle, PagingData.from(dataList))

            }
        }

    }


    companion object {

        @JvmStatic
        fun getFragment() = AnswersFragment()
    }
}