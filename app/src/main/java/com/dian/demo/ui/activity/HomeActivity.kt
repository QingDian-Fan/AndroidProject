package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.demo.project.utils.ext.gone
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityHomeBinding
import com.dian.demo.ui.adapter.HomePagerAdapter
import com.dian.demo.ui.fragment.HomeFragment
import com.dian.demo.ui.fragment.SettingFragment
import com.dian.demo.utils.ResourcesUtil
import kotlin.system.exitProcess


class HomeActivity : BaseAppBindActivity<ActivityHomeBinding>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, HomeActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    private val fragmentList by lazy {
        listOf(HomeFragment.getFragment(), SettingFragment.getFragment())
    }


    override fun getLayoutId(): Int = R.layout.activity_home

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().leftImageButton.visibility = gone
        setPageTitle("首页")
        binding.vpContent.adapter = HomePagerAdapter(fragmentList, supportFragmentManager)
        binding.tabHome.setOnItemSelectedListener {
            getTitleBarView().setCenterText(it.title)
            when (it.itemId) {
                R.id.tab_home -> {
                    binding.vpContent.currentItem = 0
                }
                R.id.tab_setting -> {
                    binding.vpContent.currentItem = 1
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