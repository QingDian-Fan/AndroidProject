package com.dian.demo.ui.adapter

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import com.dian.demo.R
import com.dian.demo.di.model.NavigationData
import com.dian.demo.utils.ResourcesUtil
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView

class KnowledgeTabAdapter(val titleList: MutableList<NavigationData>): CommonNavigatorAdapter() {
    override fun getCount(): Int = titleList.size

    override fun getTitleView(
        context: Context?,
        index: Int,
    ): IPagerTitleView? {
        val titleView = ColorTransitionPagerTitleView(context).apply {
            text = titleList[index].name
            normalColor = Color.GRAY
            selectedColor = Color.BLACK
            setTextSize(TypedValue.COMPLEX_UNIT_PX, ResourcesUtil.getDimens(R.dimen.dp_15))
        }
        return titleView
    }

    override fun getIndicator(context: Context?): IPagerIndicator? {
        return LinePagerIndicator(context).apply {
            setColors(Color.TRANSPARENT)
            lineHeight = 0f
        }
    }
}