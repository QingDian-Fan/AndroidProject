package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityContainerBinding
import com.dian.demo.ui.titlebar.CommonTitleBar

class ContainerActivity : BaseAppBindActivity<ActivityContainerBinding>() {
    companion object {
        fun start(mContext: Context, isWebListPage: Boolean, mWebListPage: Int) {
            val intent = Intent()
            intent.setClass(mContext, ContainerActivity::class.java)
            intent.putExtra("isWebListPage", isWebListPage)
            intent.putExtra("WEB_LIST_PAGE", mWebListPage)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_container

    override fun initialize(savedInstanceState: Bundle?) {
        val mWebListPage = intent.getIntExtra("WEB_LIST_PAGE", 0)
        val isWebListPage = intent.getBooleanExtra("isWebListPage", true)
        if (isWebListPage) {
            val bundle = Bundle().apply {
                putInt("WEB_LIST_PAGE", mWebListPage)
            }
            supportFragmentManager.setFragmentResult("KEY_WEB_LIST_PAGE", bundle)
        } else {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
            val navController = navHostFragment.navController
            val navGraph = navController.navInflater.inflate(R.navigation.navigation_setting)
            navGraph.startDestination = R.id.settingFragment
            navController.graph = navGraph

            navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
                if (destination.id == R.id.settingFragment) {
                    runOnUiThread {
                        setPageTitle("设置")
                    }
                }else{
                    setPageTitle("关于我们")
                }
            }
        }

        getTitleBarView().setListener { v, action, extra ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressed()
            }
        }
    }
}