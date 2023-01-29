package com.dian.demo.ui.activity


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.databinding.ActivityWebExplorerBinding
import com.dian.demo.ui.fragment.WebFragment
import com.dian.demo.ui.titlebar.CommonTitleBar
import com.dian.demo.utils.ResourcesUtils
import com.dian.demo.utils.share.dialog.ShareDialog


open class WebExplorerActivity : BaseAppBindActivity<ActivityWebExplorerBinding>() {

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"
        private const val EXTRA_TITLE = "EXTRA_TITLE"


        @JvmStatic
        fun start(
            mContext: Context, urlString: String,
            titleString: String = ResourcesUtils.getString(R.string.app_name)
        ) {
            val intent = Intent()
            intent.setClass(mContext, WebExplorerActivity::class.java)
            intent.putExtra(EXTRA_URL, urlString)
            intent.putExtra(EXTRA_TITLE, titleString)
            mContext.startActivity(intent)
        }
    }

    private var urlString: String? = null

    private var titleString: String? = null
    private var mWebFragment: WebFragment? = null

    override fun getLayoutId(): Int = R.layout.activity_web_explorer

    /**
     *  初始化操作
     */
    override fun initialize(savedInstanceState: Bundle?) {
        urlString = intent.getStringExtra(EXTRA_URL)
        titleString = intent.getStringExtra(EXTRA_TITLE)
        setPageTitle(
            if (titleString != null && !TextUtils.isEmpty(titleString)) titleString!! else ResourcesUtils.getString(
                R.string.app_name
            )
        )

        mWebFragment = WebFragment.getFragment(urlString!!)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_container, mWebFragment!!)
            .commitAllowingStateLoss()
        initShare()
        getTitleBarView().setListener { _, action, _ ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressed()
            } else if (action == CommonTitleBar.ACTION_RIGHT_BUTTON) {
                val dialogFragment = ShareDialog()
                // dialogFragment.setText(true,"wanAndroid")
                // dialogFragment.setBitmapData(ScreenShotUtils.shotActivityNoStatusBar(this))
                val mBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_pink)
                dialogFragment.setLinkData(
                    true,
                    mBitmap,
                    mWebFragment!!.getCurrentUrlString(),
                    ResourcesUtils.getString(R.string.app_name),
                    mWebFragment!!.getCurrentTitleString()
                )
                dialogFragment.show(supportFragmentManager, "")
            }
        }

        setPageRightIcon(R.mipmap.icon_share)

    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (mWebFragment != null) {
            if (keyCode == KeyEvent.KEYCODE_BACK && mWebFragment!!.canGoBack()) {
                mWebFragment!!.doActionBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

}