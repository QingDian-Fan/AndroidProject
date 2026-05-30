package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import com.common.ui.BaseAppBindActivity
import com.common.utils.ResourcesUtil
import com.demo.project.R
import com.demo.project.databinding.ActivityWebExplorerBinding
import com.demo.project.ui.fragment.WebExplorerFragment

class WebExplorerActivity : BaseAppBindActivity<ActivityWebExplorerBinding>() {

    companion object {
        private const val EXTRA_URL = "urlString"

        @JvmStatic
        fun start(mContext: Context, urlString: String) {
            val intent = Intent(mContext, WebExplorerActivity::class.java)
                .putExtra(EXTRA_URL, urlString)
                .apply {
                    if (mContext !is Activity) {
                        flags=FLAG_ACTIVITY_NEW_TASK
                    }
                }
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_web_explorer

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView()?.apply {
            getMainView()?.visibility = View.GONE
            setOpenStatusBar(true)
            setStatusBarColor(ResourcesUtil.getColor(R.color.text_blue_color))
            getBottomLine()?.visibility = View.GONE
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_container, WebExplorerFragment.getFragment())
                .commitAllowingStateLoss()
        }

        val bundle = Bundle().apply {
            putString(EXTRA_URL, intent.getStringExtra(EXTRA_URL))
        }
        supportFragmentManager.setFragmentResult(WebExplorerFragment.KEY_URL_DATA, bundle)
    }
}
