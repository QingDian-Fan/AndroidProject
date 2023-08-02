package com.dian.demo.ui.img

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import androidx.viewpager2.widget.ViewPager2
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityImagePreviewBinding
import com.dian.demo.ui.activity.DemoActivity
import java.io.File
import java.util.ArrayList
import java.util.HashMap

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

    override fun getLayoutId(): Int = R.layout.activity_image_preview

    override fun initialize(savedInstanceState: Bundle?) {

        getTitleBarView().visibility = gone
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
        super.onDestroy()
        binding.viewPager.unregisterOnPageChangeCallback(mPageChangeCallback)
    }

    private val mPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.tvImagePreviewIndicator.text = "${(position + 1)}/${mAdapter?.itemCount}"
        }
    }

}