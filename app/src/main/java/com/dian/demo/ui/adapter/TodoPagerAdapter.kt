package com.dian.demo.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

class TodoPagerAdapter(private val fragmentList :List<Fragment>, fm: FragmentManager):FragmentStatePagerAdapter(fm) {
    override fun getCount(): Int = fragmentList.size

    override fun getItem(position: Int): Fragment = fragmentList[position]
}