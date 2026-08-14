package com.demo.project.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import com.common.share.dialog.ShareDialog
import com.common.ui.BaseAppBindActivity
import com.common.utils.ResourcesUtil
import com.common.utils.code.generate.GenerateCodeUtils
import com.common.weight.titlebar.CommonTitleBar
import com.demo.project.R
import com.demo.project.databinding.ActivityWebBinding
import com.demo.project.databinding.LayoutShareFooterBinding
import com.demo.project.ui.fragment.WebFragment
import com.dian.annotation.RequireLogin

@RequireLogin
class WebActivity : BaseAppBindActivity<ActivityWebBinding>() {

    companion object {
        private const val EXTRA_URL = "EXTRA_URL"
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val DEFAULT_URL = "https://www.baidu.com"

        @JvmStatic
        fun start(
            mContext: Context,
            urlString: String = DEFAULT_URL,
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
        val fragment = webFragment ?: return
        val isShareBitmap = true
        fragment.getShareData { url, covers, title, desc ->
            val shareUrl = url.ifEmpty { fragment.getCurrentUrlString() }
            val shareTitle = title.ifEmpty {
                fragment.getCurrentTitleString().ifEmpty { getString(R.string.app_name) }
            }
            val shareDesc = desc.ifEmpty { shareUrl }

            if (isShareBitmap){
                val shareBinding = LayoutShareFooterBinding.inflate(LayoutInflater.from(this@WebActivity))
                val codeBitmap = shareUrl.takeIf { it.isNotEmpty() }
                    ?.let { GenerateCodeUtils.createQRCodeBitmap(it, null, 1024, 150) }
                shareBinding.ivCode.setImageBitmap(codeBitmap)
                shareBinding.title.text = shareTitle
                shareBinding.tvContent.text = shareDesc
                val shareBitmap = createShareBitmap(shareBinding.root,binding.flContainer)
                shareBinding.ivCode.setImageDrawable(null)
                codeBitmap?.recycle()
                if (shareBitmap == null) {
                    showToast(R.string.share_denied)
                    return@getShareData
                }
                ShareDialog().shareBitmapData(shareBitmap).show(supportFragmentManager, "")
            }else{
                val coverUrl = covers.firstOrNull().orEmpty()
                if (coverUrl.isNotEmpty()) {
                    ShareDialog().shareLinkData(true, shareUrl, coverUrl, shareTitle, shareDesc).show(supportFragmentManager, "")
                } else {
                    val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                    ShareDialog().shareLinkData(true, bitmap, shareUrl, shareTitle, shareDesc).show(supportFragmentManager, "")
                }
            }
        }
    }

    private fun createShareBitmap(footerView: View, contentView: View): Bitmap? {
        val width = contentView.width
        val contentHeight = contentView.height
        if (width <= 0 || contentHeight <= 0) return null

        footerView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val footerHeight = footerView.measuredHeight
        if (footerHeight <= 0) return null
        footerView.layout(0, 0, width, footerHeight)

        val totalHeight = contentHeight.toLong() + footerHeight
        if (totalHeight > Int.MAX_VALUE) return null
        val bitmap = Bitmap.createBitmap(width, totalHeight.toInt(), Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        contentView.draw(canvas)
        canvas.translate(0f, contentHeight.toFloat())
        footerView.draw(canvas)
        return bitmap
    }
}
