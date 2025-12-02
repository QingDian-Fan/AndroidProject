package com.dian.demo.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.dian.demo.di.model.NavigationData
import com.dian.demo.ui.fragment.KnowledgeChildDetailFragment
import com.dian.demo.ui.fragment.KnowledgeDetailFragment

class KnowledgePagerAdapter(private val titleList: MutableList<NavigationData>, fm:FragmentManager):
    FragmentStatePagerAdapter(fm) {

    override fun getCount(): Int = titleList.size

    override fun getItem(position: Int): Fragment = KnowledgeChildDetailFragment.getFragment(titleList.getOrNull(position))
}