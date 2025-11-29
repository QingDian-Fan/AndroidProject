package com.dian.demo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMFragment
import com.dian.demo.databinding.FragmentSearchResultBinding
import com.dian.demo.di.vm.SearchViewModel


class SearchResultFragment : BaseAppVMFragment<FragmentSearchResultBinding, SearchViewModel>() {

    override fun createViewModel(): SearchViewModel = SearchViewModel()

    override fun getLayoutId(): Int = R.layout.fragment_search_result

    override fun initialize(savedInstanceState: Bundle?) {

    }


    companion object {

        @JvmStatic
        fun getFragment() = SearchResultFragment()
    }
}