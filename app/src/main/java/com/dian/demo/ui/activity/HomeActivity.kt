package com.dian.demo.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.KeyEvent
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityHomeBinding
import com.dian.demo.ui.adapter.HomePagerAdapter
import com.dian.demo.ui.fragment.AnswersFragment
import com.dian.demo.ui.fragment.HomeFragment
import com.dian.demo.ui.fragment.MineFragment
import com.dian.demo.ui.fragment.SetupFragment
import com.dian.demo.ui.titlebar.CommonTitleBar
import com.dian.demo.utils.ResourcesUtil
import com.project.common.utils.ext.gone
import com.project.common.utils.ext.visible
import kotlin.system.exitProcess


class HomeActivity : BaseAppBindActivity<ActivityHomeBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, HomeActivity::class.java)
            if (mContext !is Activity) {
                intent.flags = FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            mContext.startActivity(intent)
        }
    }

    private val fragmentList by lazy {
        listOf(
            HomeFragment.getFragment(),
            AnswersFragment.getFragment(),
            SetupFragment.getFragment(),
            MineFragment.getFragment()
        )
    }


    override fun getLayoutId(): Int = R.layout.activity_home

    override fun initialize(savedInstanceState: Bundle?) {

        getTitleBarView()?.leftImageButton?.visibility = gone
        setPageTitle("首页")
        getTitleBarView()?.setLeftIcon(R.mipmap.ic_scan)
        getTitleBarView()?.setRightIcon(R.mipmap.ic_search)
        getTitleBarView()?.setListener { _, action, _ ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                ScanActivity.start(this@HomeActivity)
            }else if (action == CommonTitleBar.ACTION_RIGHT_BUTTON){
                SearchActivity.start(this@HomeActivity)
            }
        }
        binding.vpContent.offscreenPageLimit = fragmentList.size
        binding.vpContent.adapter = HomePagerAdapter(fragmentList, supportFragmentManager)
        binding.tabHome.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.tab_home -> {
                    getTitleBarView()?.mainView.visible()
                    getTitleBarView()?.bottomLine.visible()
                    getTitleBarView()?.setCenterText(it.title)
                    getTitleBarView()?.leftImageButton.visible()
                    getTitleBarView()?.rightImageButton.visible()
                    getTitleBarView()?.setLeftIcon(R.mipmap.ic_scan)
                    getTitleBarView()?.setRightIcon(R.mipmap.ic_search)
                    binding.vpContent.currentItem = 0
                }

                R.id.tab_answers -> {
                    getTitleBarView()?.mainView.visible()
                    getTitleBarView()?.bottomLine.visible()
                    getTitleBarView()?.setCenterText(it.title)
                    getTitleBarView()?.leftImageButton.gone()
                    getTitleBarView()?.rightImageButton.gone()
                    binding.vpContent.currentItem = 1
                }

                R.id.tab_setup -> {
                    getTitleBarView()?.mainView.gone()
                    getTitleBarView()?.bottomLine.gone()
                    getTitleBarView()?.leftImageButton.gone()
                    getTitleBarView()?.rightImageButton.gone()
                    binding.vpContent.currentItem = 2
                }

                R.id.tab_mine -> {
                    getTitleBarView()?.mainView.visible()
                    getTitleBarView()?.bottomLine.visible()
                    getTitleBarView()?.setCenterText(it.title)
                    getTitleBarView()?.leftImageButton.gone()
                    getTitleBarView()?.rightImageButton.gone()
                    binding.vpContent.currentItem = 3
                }
            }
            true
        }
    }

    private val mDuraction = 2000 // 两次返回键之间的时间差

    var mLastTime: Long = 0 // 最后一次按back键的时刻

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) { // 截获back事件
            exitApp()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    private fun exitApp() = if (System.currentTimeMillis() - mLastTime > mDuraction) {
        showToast(ResourcesUtil.getString(R.string.exit_app))
        mLastTime = System.currentTimeMillis()
    } else {
        //onBackPressed()
        exitProcess(0)
    }
}