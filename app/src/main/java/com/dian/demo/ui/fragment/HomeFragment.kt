package com.dian.demo.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dian.demo.ProjectApplication
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentHomeBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.BannerBean
import com.dian.demo.di.vm.HomeViewModel
import com.dian.demo.ui.activity.WebExplorerActivity
import com.dian.demo.ui.adapter.GlobalArticleAdapter
import com.dian.demo.ui.adapter.HomeArticleAdapter
import com.dian.demo.utils.CustomDividerItemDecoration
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.SmartRefreshUtil
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.Utils
import com.dian.demo.utils.ext.observeNonNull
import com.stx.xhb.androidx.XBanner
import com.stx.xhb.androidx.transformers.Transformer


class HomeFragment : BaseAppVMFragment<FragmentHomeBinding, HomeViewModel>() {

    companion object {
        @JvmStatic
        fun getFragment() = HomeFragment()
    }

    override fun getViewModelClass(): Class<HomeViewModel> = HomeViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_home

    private var page = 0
    private var mAdapter: GlobalArticleAdapter? = null
    private val dataList: ArrayList<ArticleBean> by lazy { arrayListOf() }

    override fun initialize(savedInstanceState: Bundle?) {
        viewModel.getBannerList()
        viewModel.bannerData.observeNonNull(this) {
            binding.bannerView.setBannerData(R.layout.item_home_banner, it)
            binding.bannerView.loadImage { banner, model, view, position ->
                Glide.with(requireActivity())
                    .load(it[position].imagePath)
                    .error(R.mipmap.action_back)
                    .centerCrop()
                    .into(view as AppCompatImageView)
            }
            binding.bannerView.setOnItemClickListener { banner, model, view, position ->
                it[position].url?.let {
                    WebExplorerActivity.start(requireContext(), it, it)
                }
            }
        }
        binding.bannerView.setPageTransformer(Transformer.Default)
        SmartRefreshUtil.with(binding.layoutRefresh).autoRefresh()
        SmartRefreshUtil.with(binding.layoutRefresh).setRefreshListener {
            page = 0
            viewModel.getArticleList(page = page)
        }
        SmartRefreshUtil.with(binding.layoutRefresh).setLoadMoreListener {
            page = page + 1
            viewModel.getArticleList(page)
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
                mAdapter?.setCollectListener { position,isChecked,bean->
                    bean.collect = isChecked
                    if (isChecked){
                        viewModel.collectArticle(bean.id.toString())
                    }else{
                        viewModel.cancelCollectArticle(bean.id.toString())
                    }
                }
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


    override fun onResume() {
        super.onResume()
        binding.bannerView.startAutoPlay();
    }

    override fun onStart() {
        super.onStart()
        binding.bannerView.stopAutoPlay();
    }
}