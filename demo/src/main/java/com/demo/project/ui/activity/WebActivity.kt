package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import com.common.share.dialog.ShareDialog
import com.common.ui.BaseAppBindActivity
import com.common.utils.ResourcesUtil
import com.common.weight.titlebar.CommonTitleBar
import com.demo.project.R
import com.demo.project.databinding.ActivityWebBinding
import com.demo.project.ui.fragment.WebFragment

class WebActivity : BaseAppBindActivity<ActivityWebBinding>() {

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val DEFAULT_URL = "https://www.baidu.com"

        @JvmStatic
        fun start(
            mContext: Context,
            urlString: String,
            titleString: String? = null
        ) {
            val intent = Intent(mContext, WebActivity::class.java)
                .putExtra(EXTRA_URL, urlString)
                .apply {
                    titleString?.takeIf { it.isNotEmpty() }?.let {
                        putExtra(EXTRA_TITLE, it)
                    }
                    if (mContext !is Activity) {
                        flags=FLAG_ACTIVITY_NEW_TASK
                    }
                }
            mContext.startActivity(intent)
        }
    }

    private var webFragment: WebFragment? = null

    override fun getLayoutId(): Int = R.layout.activity_web

    override fun initialize(savedInstanceState: Bundle?) {
        val urlString = resolveInitialUrl()
        val titleString = intent.getStringExtra(EXTRA_TITLE)

        setPageTitle(resolveInitialTitle(titleString))
        setPageRightIcon(R.mipmap.icon_share)
        getTitleBarView()?.setListener { _, action, _ ->
            when (action) {
                CommonTitleBar.ACTION_LEFT_BUTTON -> handleBack()
                CommonTitleBar.ACTION_RIGHT_BUTTON -> showShareDialog()
            }
        }

        if (savedInstanceState == null) {
            webFragment = WebFragment.getFragment(urlString)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fl_container, webFragment!!)
                .commitAllowingStateLoss()
        } else {
            webFragment = supportFragmentManager.findFragmentById(R.id.fl_container) as? WebFragment
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val fragment = webFragment
        if (keyCode == KeyEvent.KEYCODE_BACK && fragment != null && fragment.canGoBack()) {
            fragment.doActionBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun resolveInitialUrl(): String {
        if (Intent.ACTION_VIEW == intent.action) {
            intent.data?.toString()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        return intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotEmpty() } ?: DEFAULT_URL
    }

    private fun resolveInitialTitle(titleString: String?): String {
        return titleString?.takeIf { !TextUtils.isEmpty(it) }
            ?: ResourcesUtil.getString(R.string.app_name)
    }

    private fun handleBack() {
        val fragment = webFragment
        if (fragment != null && fragment.canGoBack()) {
            fragment.doActionBack()
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun showShareDialog() {
        webFragment?.getShareData { url, covers, title, desc ->
            val shareUrl = url.ifEmpty { webFragment?.getCurrentUrlString().orEmpty() }
            val shareTitle = title.ifEmpty {
                webFragment?.getCurrentTitleString().orEmpty()
                    .ifEmpty { ResourcesUtil.getString(R.string.app_name) }
            }
            val shareDesc = desc.ifEmpty { shareUrl }
            val dialog = ShareDialog()
            val coverUrl = covers.firstOrNull().orEmpty()
            if (coverUrl.isNotEmpty()) {
                dialog.setLinkData(true, shareUrl, coverUrl, shareTitle, shareDesc)
            } else {
                val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                dialog.setLinkData(true, bitmap, shareUrl, shareTitle, shareDesc)
            }
            dialog.show(supportFragmentManager, "")
        }
    }
}
