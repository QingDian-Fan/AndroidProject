package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityContainerBinding
import com.dian.demo.di.model.NavigationData
import com.dian.demo.ui.titlebar.CommonTitleBar
import com.dian.demo.utils.MoshiUtil
import com.squareup.moshi.Moshi

class ContainerActivity : BaseAppBindActivity<ActivityContainerBinding>() {
    companion object {
        fun start(mContext: Context, isWebListPage: Boolean, mWebListPage: Int) {
            val intent = Intent()
            intent.setClass(mContext, ContainerActivity::class.java)
            intent.putExtra("isWebListPage", isWebListPage)
            intent.putExtra("WEB_LIST_PAGE", mWebListPage)
            mContext.startActivity(intent)
        }

        fun start(
            mContext: Context,
            mPage: Int,
            titleList: List<NavigationData>?,
            data: NavigationData?,
        ) {
            val intent = Intent()
            intent.setClass(mContext, ContainerActivity::class.java)
            intent.putExtra("mPage", mPage)
            titleList?.let {
                val dataListString = MoshiUtil.toJsonList<NavigationData>(it)
                intent.putExtra("dataListString", dataListString)
            }
            data?.let {
                val dataString = MoshiUtil.toJson<NavigationData>(it)
                intent.putExtra("dataString", dataString)
            }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_container

    override fun initialize(savedInstanceState: Bundle?) {
        val mPage = intent?.getIntExtra("mPage", -1)
        if (mPage == 0) {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
            val navController = navHostFragment.navController
            val navGraph = navController.navInflater.inflate(R.navigation.navigation_setting)
            navGraph.startDestination = R.id.knowledgeDetailFragment
            navController.graph = navGraph
            val dataListString =intent.getStringExtra("dataListString")
            val dataString =intent.getStringExtra("dataString")
            val bundle = Bundle().apply {
                putString("dataListString", dataListString)
                putString("dataString", dataString)
            }
            supportFragmentManager.setFragmentResult("KEY_KNOWLEDGE_LIST_PAGE", bundle)
            return
        }

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
                } else {
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