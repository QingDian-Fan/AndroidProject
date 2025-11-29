package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityContainerBinding
import com.dian.demo.ui.titlebar.CommonTitleBar

class ContainerActivity: BaseAppBindActivity<ActivityContainerBinding>() {
    companion object {
        fun start(mContext: Context,mWebListPage: Int) {
            val intent = Intent()
            intent.setClass(mContext, ContainerActivity::class.java)
            intent.putExtra("WEB_LIST_PAGE",mWebListPage)
            mContext.startActivity(intent)
        }
    }
    override fun getLayoutId(): Int = R.layout.activity_container

    override fun initialize(savedInstanceState: Bundle?) {
        val mWebListPage = intent.getIntExtra("WEB_LIST_PAGE",0)
        val bundle = Bundle().apply {
            putInt("WEB_LIST_PAGE", mWebListPage)
        }
        supportFragmentManager.setFragmentResult("KEY_WEB_LIST_PAGE", bundle)
        getTitleBarView().setListener { v, action, extra ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressed()
            }
        }
    }
}