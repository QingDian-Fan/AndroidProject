package com.dian.demo.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.NavHostFragment
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMActivity
import com.dian.demo.databinding.ActivitySearchBinding
import com.dian.demo.di.model.SearchRecord
import com.dian.demo.di.vm.SearchData
import com.dian.demo.di.vm.SearchViewModel
import com.dian.demo.utils.ResourcesUtil
import com.project.common.utils.ext.gone
import com.project.common.utils.ext.observeNonNull
import com.project.common.utils.ext.visible

class SearchActivity : BaseAppVMActivity<ActivitySearchBinding, SearchViewModel>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, SearchActivity::class.java)
            if (mContext !is Activity) {
                intent.flags = FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_search

    override fun createViewModel(): SearchViewModel = SearchViewModel()


    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.mainView.gone()
        getTitleBarView()?.bottomLine.gone()
        getTitleBarView()?.setOpenStatusBar(true)
        getTitleBarView()?.setStatusBarColor(ResourcesUtil.getColor(R.color.colorPink))
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        initView()
        initData()
    }

    private fun initView() {
        binding.etSearch.addTextChangedListener {
            runOnUiThread {
                if (it?.isNotEmpty()?:false){
                    binding.aivClear.visible()
                }else{
                    binding.aivClear.gone()
                }
            }
        }
        binding.aivClear.setOnClickListener {
            binding.etSearch.setText(null)
        }
        binding.btnSearch.setOnClickListener {
            val keyword = binding.etSearch.text.toString()
            if (keyword.isNotEmpty()) {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
                navHostFragment.navController.navigate(R.id.searchResultFragment)
                viewModel.putHistoryRecord(SearchRecord(0, null, keyword, "", ""))
                viewModel.loadData.value = SearchData(0, keyword)
                viewModel.getSearchList(0, keyword)
            }
        }
    }

    private fun initData() {
        viewModel.clickData.observeNonNull(this) {
            it.name?.let { name ->
                binding.etSearch.setText(name)
            }
        }
    }

}