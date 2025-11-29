package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentWebListBinding
import com.dian.demo.ui.adapter.WebRecordAdapter
import com.dian.demo.utils.webview.bean.WebDataEntry
import com.dian.demo.utils.webview.utils.CollectWebPageUtil
import com.dian.demo.utils.webview.utils.WebBookMarkUtil
import com.dian.demo.utils.webview.utils.WebHistoryUtil

class WebListFragment : BaseAppBindFragment<FragmentWebListBinding>() {
    override fun getLayoutId(): Int = R.layout.fragment_web_list

    override fun initialize(savedInstanceState: Bundle?) {
        requireActivity().supportFragmentManager.setFragmentResultListener("KEY_WEB_LIST_PAGE", viewLifecycleOwner) { key, bundle ->
            val mWebListPage = bundle.getInt("WEB_LIST_PAGE")
            when (mWebListPage) {
                0 -> {
                    setPageTitle("阅读历史")
                    val mDataList = WebHistoryUtil.getWebHistoryList()
                    initData(mDataList)
                }
                1 -> {
                    setPageTitle("书签")
                    val mDataList = WebBookMarkUtil.getMarkWebPage()
                    initData(mDataList)
                }
                2 -> {
                    setPageTitle("收藏")
                    val mDataList = CollectWebPageUtil.getCollectWebPage()
                    initData(mDataList)
                }
            }
        }
    }

    private fun initData(dataList: MutableList<WebDataEntry>) {
        if (dataList.isEmpty()){
            showEmptyView(true)
            return
        }
       binding.rvData.apply {
           layoutManager = LinearLayoutManager(requireActivity())
           adapter = WebRecordAdapter(dataList)
       }
    }
}