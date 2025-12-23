package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentMineListBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.vm.MineListViewModel
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.ui.adapter.GlobalArticleAdapter
import com.dian.demo.utils.CustomDividerItemDecoration
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.swape.RecyclerTouchListener


class MineListFragment : BaseAppVMFragment<FragmentMineListBinding, MineListViewModel>() {

    private var page = 0
    private var mAdapter: GlobalArticleAdapter? = null
    private val dataList: ArrayList<ArticleBean> by lazy { arrayListOf() }

    override fun getLayoutId(): Int = R.layout.fragment_mine_list
    override fun getViewModelClass(): Class<MineListViewModel> = MineListViewModel::class.java

    private var mListPage = 0

    override fun initialize(savedInstanceState: Bundle?) {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "KEY_LIST_PAGE",
            viewLifecycleOwner
        ) { key, bundle ->
            mListPage = bundle.getInt("LIST_PAGE")
            initData()
        }
    }

    private fun initData() {
        SmartRefreshUtil.with(binding.layoutRefresh).autoRefresh()
        SmartRefreshUtil.with(binding.layoutRefresh).setRefreshListener {
            page = 0
            if (mListPage == 0) {
                viewModel.getMineShareData(page = page)
            } else {
                viewModel.getMineCollectData(page)
            }

        }
        SmartRefreshUtil.with(binding.layoutRefresh).setLoadMoreListener {
            page += 1
            if (mListPage == 0) {
                viewModel.getMineShareData(page = page)
            } else {
                viewModel.getMineCollectData(page)
            }
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

                val onTouchListener = RecyclerTouchListener(activity, binding.rvData)
                onTouchListener.setSwipeOptionViews(R.id.rl_change)
                    .setSwipeable(R.id.rl_article, R.id.rl_change) { viewID, position ->
                        showToast("点击了：$position")
                        val mArticleId = mAdapter?.currentList?.getOrNull(position)?.id
                        if (mListPage == 0) {
                            mArticleId?.let {
                                viewModel.deleteShareArticle(it.toString())
                                val mergeList= mAdapter?.currentList?.toMutableList()
                                mergeList?.remove(mAdapter?.currentList?.getOrNull(position))
                                mAdapter?.submitList(mergeList)
                            }
                        }else{
                            mArticleId?.let {
                                viewModel.cancelCollectArticle(it.toString())
                               val mergeList= mAdapter?.currentList?.toMutableList()
                                mergeList?.remove(mAdapter?.currentList?.getOrNull(position))
                                 mAdapter?.submitList(mergeList)
                            }
                        }
                    }
                binding.rvData.addOnItemTouchListener(onTouchListener)
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
        fun getFragment() = MineListFragment()
    }
}