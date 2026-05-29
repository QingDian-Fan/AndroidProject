package com.common.media.picker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.common.media.picker.databinding.ActivityImagePreviewBinding
import com.common.ui.BaseAppBindActivity
import java.util.ArrayList

class ImagePreviewActivity : BaseAppBindActivity<ActivityImagePreviewBinding>() {

    companion object {
        fun start(mContext: Context, dataList: ArrayList<String>, position: Int) {
            val intent = Intent()
            intent.setClass(mContext, ImagePreviewActivity::class.java)
            intent.putExtra("position", position)
            intent.putExtra("dataList", dataList)
            mContext.startActivity(intent)
        }
    }

    private var mAdapter: ImagePreviewAdapter? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup): ActivityImagePreviewBinding =
        ActivityImagePreviewBinding.inflate(inflater, container, false)

    override fun initialize(savedInstanceState: Bundle?) {

        getTitleBarView()?.visibility = View.GONE
        val position = intent.getIntExtra("position", 0)
        val dataList = intent.getStringArrayListExtra("dataList")
        binding.viewPager.offscreenPageLimit = 3
        mAdapter = ImagePreviewAdapter(this@ImagePreviewActivity, dataList!!)
        binding.viewPager.adapter = mAdapter
        binding.viewPager.setCurrentItem(position, false)
        if (dataList.size > 10) {
            binding.viewPager.registerOnPageChangeCallback(mPageChangeCallback)
        }
        binding.tvImagePreviewIndicator.text = "${(position + 1)}/${mAdapter?.itemCount}"
    }

    override fun onDestroy() {
        // 必须在 super.onDestroy() 之前访问 binding（基类会在 onDestroy 中置空 binding）
        binding.viewPager.unregisterOnPageChangeCallback(mPageChangeCallback)
        super.onDestroy()
    }

    private val mPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.tvImagePreviewIndicator.text = "${(position + 1)}/${mAdapter?.itemCount}"
        }
    }

}