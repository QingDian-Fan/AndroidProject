package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentKnowledgeDetailBinding
import com.dian.demo.di.model.NavigationData
import com.dian.demo.di.vm.KnowledgeChildDetailViewModel
import com.dian.demo.utils.MoshiUtil


class KnowledgeChildDetailFragment :
    BaseAppVMFragment<FragmentKnowledgeDetailBinding, KnowledgeChildDetailViewModel>() {

    override fun getViewModelClass(): Class<KnowledgeChildDetailViewModel> =
        KnowledgeChildDetailViewModel::class.java

    override fun getLayoutId(): Int = R.layout.fragment_knowledge_child_detail
    var data: NavigationData? = null
    override fun initialize(savedInstanceState: Bundle?) {
        val dataString = arguments?.getString("dataString")
        dataString?.let {
            data = MoshiUtil.fromJson<NavigationData>(dataString)
        }

    }

    override fun lazyInit() {
        super.lazyInit()

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