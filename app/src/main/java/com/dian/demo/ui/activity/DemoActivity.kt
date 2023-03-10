package com.dian.demo.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
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
import com.dian.demo.config.Constant.apkPath
import com.dian.demo.databinding.ActivityDemoBinding
import com.dian.demo.di.model.ArticleBean
import com.dian.demo.di.model.BannerBean
import com.dian.demo.di.model.ListData
import com.dian.demo.di.vm.DemoViewModel
import com.dian.demo.ui.dialog.*
import com.dian.demo.ui.img.ImageCancelListener
import com.dian.demo.ui.img.ImageSelectActivity
import com.dian.demo.ui.img.ImageSelectListener
import com.dian.demo.ui.img.ImageSelectUtil
import com.dian.demo.utils.*
import com.dian.demo.utils.aop.CheckPermissions
import com.dian.demo.utils.aop.SingleClick
import com.dian.demo.utils.ext.showAllowStateLoss
import com.dian.demo.utils.gray.GlobalGray
import com.dian.demo.utils.share.dialog.ShareDialog
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess
import com.dian.demo.http.Result
import com.dian.demo.http.moshi.NullSafeKotlinJsonAdapterFactory
import com.dian.demo.http.moshi.NullSafeStandardJsonAdapters
import com.dian.demo.utils.datastore.AppDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types


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

        val isGray = AppDataStore.getData("isGray", false)
        binding.btnGray.text = "????????????${isGray}"

        StatusBarUtil.setColor(this@DemoActivity, ResourcesUtil.getColor(R.color.bg_common), 0)
        StatusBarUtil.setLightMode(this@DemoActivity)

        binding.tvSpan.text = SpannableStringUtil.Builder()
            .append("????????????")
            .append("??????????????????")
            .setForegroundColor(ResourcesUtil.getColor(R.color.text_blue_color))
            .setClickSpan(object : ClickableSpan() {
                override fun onClick(mView: View) {
                    WebExplorerActivity.start(
                        this@DemoActivity,
                        ResourcesUtil.getString(R.string.app_website)
                    )
                }
            })
            .append("???")
            .append("??????????????????")
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

    private val mDuraction = 2000 // ?????????????????????????????????

    var mLastTime: Long = 0 // ???????????????back????????????


    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) { // ??????back??????
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
                            ToastUtil.showToast(this@DemoActivity, "?????????")
                        }
                    })
                    .create()
            }

            R.id.btn_network_request -> {
                viewModel.getArticleList(0)
            }
            R.id.btn_network_request_post -> {
                viewModel.doLogin("QingDian_Fan", "dian3426")
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
                    ToastUtil.showToast(str = "$year ???-$month ???-$day ???,?????????:$timeInMillis")
                }
                dialog.onCancel = {
                    ToastUtil.showToast(str = "?????????")
                }
            }
            R.id.btn_update_dialog -> {

                val apkName = "???Android.apk"
                val apkFile = File(apkPath, apkName)
                if (apkFile.exists()) {
                    TipDialog.getDialog(
                        "??????",
                        "??????????????????????????????",
                        "dian://install?filePath=$apkPath$apkName"
                    ).showAllowStateLoss(supportFragmentManager, "")
                    return
                }
                UpdateDialog.getDialog(
                    "https://cdn.mytoken.org/app_download/MT-mytoken-hk-release-3.3.4_mytoken_aligned_signed.apk",
                    "???Android.apk"
                ).showAllowStateLoss(supportFragmentManager, "")
            }
            R.id.btn_gray -> {
                val isGray = AppDataStore.getData("isGray", false)
                AppDataStore.putData("isGray", !isGray)
            }
            R.id.btn_parse -> {
                val stringBuilder = StringBuilder()
                val bufferedReader = BufferedReader(InputStreamReader(assets.open("banner.json")))
                var lineString: String?
                while (bufferedReader.readLine().also { lineString = it } != null) {
                    stringBuilder.append(lineString)
                }
                val jsonString = stringBuilder.toString()
                Log.e("TAGTAG", "jsonString--->$jsonString")
                val type = Types.newParameterizedType(
                    Result::class.java,
                    Types.newParameterizedType(List::class.java, BannerBean::class.java)
                )
                val result = MoshiUtil.fromJson<Result<List<BannerBean>>>(jsonString,type)

                Log.e("TAGTAG", "result--->" + result.toString())
            }
        }
    }

    override fun createViewModel(): DemoViewModel = DemoViewModel()
}