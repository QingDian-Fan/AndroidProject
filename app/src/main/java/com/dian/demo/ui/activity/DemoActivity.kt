package com.dian.demo.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.KeyEvent
import android.view.View
import com.demo.project.utils.ext.gone
import com.dian.demo.ProjectApplication
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindActivity
import com.dian.demo.base.BaseAppVMActivity
import com.dian.demo.databinding.ActivityDemoBinding
import com.dian.demo.di.vm.DemoViewModel
import com.dian.demo.ui.dialog.AddressDialog
import com.dian.demo.ui.dialog.DatePickDialog
import com.dian.demo.ui.dialog.DebugDialog
import com.dian.demo.ui.dialog.UpdateDialog
import com.dian.demo.ui.img.ImageCancelListener
import com.dian.demo.ui.img.ImageSelectActivity
import com.dian.demo.ui.img.ImageSelectListener
import com.dian.demo.ui.img.ImageSelectUtil
import com.dian.demo.utils.*
import com.dian.demo.utils.aop.CheckPermissions
import com.dian.demo.utils.aop.SingleClick
import com.dian.demo.utils.ext.showAllowStateLoss
import com.dian.demo.utils.share.dialog.ShareDialog


class DemoActivity : BaseAppVMActivity<ActivityDemoBinding, DemoViewModel>() {

    companion object {
        fun start(mContext: Context) {
            val intent = Intent()
            intent.setClass(mContext, DemoActivity::class.java)
            mContext.startActivity(intent)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_demo

    val mSelectList = ArrayList<String>()

    override fun initialize(savedInstanceState: Bundle?) {
        getTitleBarView().setOpenStatusBar(false)
        getTitleBarView().setCenterText(ResourcesUtil.getString(R.string.demo_title_text))
        getTitleBarView().leftImageButton.visibility = gone

        StatusBarUtil.setColor(this@DemoActivity, ResourcesUtil.getColor(R.color.bg_common), 0)
        StatusBarUtil.setLightMode(this@DemoActivity)

        binding.tvSpan.text = SpannableStringUtil.Builder()
            .append("您已同意")
            .append("《用户协议》")
            .setForegroundColor(ResourcesUtil.getColor(R.color.text_blue_color))
            .setClickSpan(object : ClickableSpan() {
                override fun onClick(mView: View) {
                    WebExplorerActivity.start(
                        this@DemoActivity,
                        ResourcesUtil.getString(R.string.app_website)
                    )
                }
            })
            .append("和")
            .append("《隐私政策》")
            .setForegroundColor(ResourcesUtil.getColor(R.color.colorPink))
            .setClickSpan(object : ClickableSpan() {
                override fun onClick(mView: View) {
                    WebExplorerActivity.start(
                        this@DemoActivity,
                        ResourcesUtil.getString(R.string.app_website)
                    )
                }
            })
            .create()
        binding.tvSpan.movementMethod = LinkMovementMethod.getInstance()
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
        onBackPressed()
    }


    @SingleClick
    fun clickView(view: View) {
        when (view.id) {
            R.id.btn_share_text -> {
                ShareDialog().setText(true, ResourcesUtil.getString(R.string.app_content))
                    .showAllowStateLoss(supportFragmentManager, "share-text")
            }
            R.id.btn_share_link -> {
                val iconLogo = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_pink)
                ShareDialog().setLinkData(
                    true,
                    iconLogo,
                    ResourcesUtil.getString(R.string.app_website),
                    ResourcesUtil.getString(R.string.app_name),
                    ResourcesUtil.getString(R.string.app_content)
                ).showAllowStateLoss(supportFragmentManager, "share-link")
            }
            R.id.btn_share_bitmap -> {
                val bitmap = ScreenShotUtil.shotActivityNoStatusBar(this)
                ShareDialog().setBitmapData(bitmap)
                    .showAllowStateLoss(supportFragmentManager, "share-bitmap")
            }
            R.id.btn_debug_activity -> {
                DebugActivity.start(this)
            }
            R.id.btn_scan_activity -> {
                ScanActivity.start(this)
            }
            R.id.btn_generate_activity -> {
                GenerateActivity.start(this)
            }
            R.id.btn_web_activity -> {
                DebugDialog().showAllowStateLoss(supportFragmentManager, "web")
            }
            R.id.btn_clear_cache -> {
                CacheUtil.clearAllCache(this@DemoActivity)
                binding.btnClearCache.postDelayed({
                    binding.btnClearCache.text = ResourcesUtil.getString(
                        R.string.cache_text, CacheUtil.getTotalCacheSize(
                            ProjectApplication.getAppContext()
                        )
                    )
                }, 500)

            }
            R.id.btn_image_select -> {

                ImageSelectUtil()
                    .setActivity(this@DemoActivity)
                    .setMaxSelect(5)
                    .setSelectList(mSelectList)
                    .setColumn(3)
                    .setSelectListener(object : ImageSelectListener {
                        override fun selectListener(selectList: ArrayList<String>) {
                            mSelectList.clear()
                            if (selectList.isNotEmpty()) {
                                selectList.forEach {
                                    mSelectList.add(it)
                                }
                            }
                        }
                    })
                    .setCancelListener(object : ImageCancelListener {
                        override fun cancel() {
                            mSelectList.clear()
                            ToastUtil.showToast(this@DemoActivity, "取消了")
                        }
                    })
                    .create()
            }
            R.id.btn_network_request -> {
                viewModel.getArticleList(0)
            }
            R.id.btn_video_play -> {
                VideoPlayerActivity.start(this@DemoActivity)
            }
            R.id.btn_address_dialog -> {
                AddressDialog.getDialog().showAllowStateLoss(supportFragmentManager, "")
            }
            R.id.btn_date_dialog -> {
                val dialog = DatePickDialog.getDialog()
                dialog.showAllowStateLoss(supportFragmentManager, "")
                dialog.onSelected = { year, month, day, timeInMillis ->
                    ToastUtil.showToast(str = "$year 年-$month 月-$day 日,时间戳:$timeInMillis")
                }
                dialog.onCancel = {
                    ToastUtil.showToast(str = "取消了")
                }
            }
            R.id.btn_update_dialog -> {
                UpdateDialog.getDialog().showAllowStateLoss(supportFragmentManager,"")
            }
        }
    }

    override fun createViewModel(): DemoViewModel = DemoViewModel()
}