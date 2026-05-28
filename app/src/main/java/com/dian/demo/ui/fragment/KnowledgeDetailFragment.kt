package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.core.os.bundleOf
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentKnowledgeDetailBinding
import com.dian.demo.di.model.NavigationData
import com.dian.demo.ui.adapter.KnowledgePagerAdapter
import com.dian.demo.ui.adapter.KnowledgeTabAdapter
import com.project.common.utils.MoshiUtil
import net.lucode.hackware.magicindicator.ViewPagerHelper
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator

class KnowledgeDetailFragment : BaseAppBindFragment<FragmentKnowledgeDetailBinding>() {
    override fun getLayoutId(): Int = R.layout.fragment_knowledge_detail

    override fun initialize(savedInstanceState: Bundle?) {
        requireActivity().supportFragmentManager.setFragmentResultListener("KEY_KNOWLEDGE_LIST_PAGE", viewLifecycleOwner) { key, bundle ->
            val dataListString = bundle.getString("dataListString")
            dataListString?.let {
                val dataList = MoshiUtil.fromJsonToList<NavigationData>(it)
                dataList?.toMutableList()?.let { titleList->
                    binding.vpContent.adapter = KnowledgePagerAdapter(titleList, childFragmentManager)
                    val commonNavigator = CommonNavigator(requireActivity())
                    commonNavigator.adapter = KnowledgeTabAdapter(titleList)
                    binding.magicIndicator.navigator = commonNavigator
                    ViewPagerHelper.bind(binding.magicIndicator, binding.vpContent)
                }
            }
        }
    }


    companion object {
        @JvmStatic
        fun getFragment(dataList: List<NavigationData>) = KnowledgeDetailFragment().apply {
            val dataListString = MoshiUtil.toJsonList(dataList)
            arguments = bundleOf("dataListString" to dataListString)
        }
    }
}