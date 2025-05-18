package com.dian.demo.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import com.dian.demo.databinding.ItemTabHomeBinding
import com.dian.demo.di.model.TabData
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.CommonPagerTitleView.OnPagerTitleChangeListener


class HomeTabAdapter(val tabList:List<TabData>): CommonNavigatorAdapter() {


    override fun getCount(): Int  = 2

    override fun getTitleView(context: Context, index: Int): IPagerTitleView {

        val commonPagerTitleView = CommonPagerTitleView(context)
        val mBinding = ItemTabHomeBinding.inflate(LayoutInflater.from(context))
        mBinding.tabImg.setImageResource(tabList[index].img)
        mBinding.tabText.text = tabList[index].title
        commonPagerTitleView.setContentView(mBinding.root)
        commonPagerTitleView.onPagerTitleChangeListener = object : OnPagerTitleChangeListener {
            override fun onSelected(index: Int, totalCount: Int) {

            }

            override fun onDeselected(index: Int, totalCount: Int) {

            }

            override fun onLeave(index: Int, totalCount: Int, leavePercent: Float, leftToRight: Boolean) {
                mBinding.tabImg.scaleX = 1.3f + (0.8f - 1.3f) * leavePercent
                mBinding.tabImg.scaleY = 1.3f + (0.8f - 1.3f) * leavePercent
            }

            override fun onEnter(index: Int, totalCount: Int, enterPercent: Float, leftToRight: Boolean) {
                mBinding.tabImg.scaleX = 0.8f + (1.3f - 0.8f) * enterPercent
                mBinding.tabImg.scaleY = 0.8f + (1.3f - 0.8f) * enterPercent
            }
        }
        return commonPagerTitleView



      /*  val customLayout: View = LayoutInflater.from(context).inflate(R.layout.item_tab_home, null)
        val tabImg = customLayout.findViewById(R.id.tab_img) as AppCompatImageView
        val tabText = customLayout.findViewById(R.id.tab_text) as AppCompatTextView*/


    }

    override fun getIndicator(context: Context?): IPagerIndicator? {
        return null
    }


}