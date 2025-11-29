package com.dian.demo.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMActivity
import com.dian.demo.databinding.ActivitySearchBinding
import com.dian.demo.di.vm.SearchViewModel
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.ext.gone

class SearchActivity : BaseAppVMActivity<ActivitySearchBinding, SearchViewModel>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, SearchActivity::class.java)
            if (mContext !is Activity) {
                intent.flags = FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_search

    override fun createViewModel(): SearchViewModel = SearchViewModel()


    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().mainView.gone()
        getTitleBarView().bottomLine.gone()
        getTitleBarView().setOpenStatusBar(true)
        getTitleBarView().setStatusBarColor(ResourcesUtil.getColor(R.color.colorPink))
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

}