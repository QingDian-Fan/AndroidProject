package com.dian.demo.ui.activity

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import com.demo.project.utils.ext.gone
import com.dian.demo.ProjectApplication
import com.dian.demo.R
import com.dian.demo.base.BaseAppVMActivity
import com.dian.demo.config.Constant.apkPath
import com.dian.demo.databinding.ActivityDemoBinding
import com.dian.demo.di.model.BannerBean
import com.dian.demo.di.vm.DemoViewModel
import com.dian.demo.http.Result
import com.dian.demo.test.proxy.ApiGenerator
import com.dian.demo.test.proxy.LoginApi
import com.dian.demo.ui.dialog.AddressDialog
import com.dian.demo.ui.dialog.DatePickDialog
import com.dian.demo.ui.dialog.DebugDialog
import com.dian.demo.ui.dialog.KeyBoardDialog
import com.dian.demo.ui.dialog.TipDialog
import com.dian.demo.ui.dialog.UpdateDialog
import com.dian.demo.ui.img.ImageCancelListener
import com.dian.demo.ui.img.ImageSelectListener
import com.dian.demo.ui.img.ImageSelectUtil
import com.dian.demo.utils.CacheUtil
import com.dian.demo.utils.MoshiUtil
import com.dian.demo.utils.ResourcesUtil
import com.dian.demo.utils.ScreenShotUtil
import com.dian.demo.utils.SpannableStringUtil
import com.dian.demo.utils.StatusBarUtil
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.aop.SingleClick
import com.dian.demo.utils.bus.LiveDataBus
import com.dian.demo.utils.datastore.AppDataStore
import com.dian.demo.utils.ext.showAllowStateLoss
import com.dian.demo.utils.mode.UIModeManager
import com.dian.demo.utils.share.dialog.ShareDialog
import com.dian.demo.utils.sse.ExecuteSSEUtil
import com.dian.demo.utils.sse.IChatListener
import com.squareup.moshi.Types
import skin.support.SkinCompatManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


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
        binding.btnGray.text = "黑白屏：${isGray}"

        StatusBarUtil.setColor(this@DemoActivity, ResourcesUtil.getColor(R.color.bg_common), 0)
        StatusBarUtil.setLightMode(this@DemoActivity)
        val api = ApiGenerator.generateApi(
            LoginApi::class.java
        )
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
                    ToastUtil.showToast(str = "$year 年-$month 月-$day 日,时间戳:$timeInMillis")
                }
                dialog.onCancel = {
                    ToastUtil.showToast(str = "取消了")
                }
            }

            R.id.btn_update_dialog -> {

                val apkName = "玩Android.apk"
                val apkFile = File(apkPath, apkName)
                if (apkFile.exists()) {
                    TipDialog.getDialog(
                        "提示",
                        "文件已下载，是否安装",
                        "dian://install?filePath=$apkPath$apkName"
                    ).showAllowStateLoss(supportFragmentManager, "")
                    return
                }
                UpdateDialog.getDialog(
                    "https://cdn.mytoken.org/app_download/MT-mytoken-hk-release-3.3.4_mytoken_aligned_signed.apk",
                    "玩Android.apk"
                ).showAllowStateLoss(supportFragmentManager, "")
            }

            R.id.btn_gray -> {
                val isGray = AppDataStore.getData("isGray", false)
                AppDataStore.putData("isGray", !isGray)
            }

            R.id.btn_default -> {
                SkinCompatManager.getInstance().restoreDefaultTheme()
                UIModeManager.getInstance().broadCastUiModeChanged(false)
                LiveDataBus.getDefault().postEvent("UI_MODE",false)
                AppDataStore.putData("UI_MODE",false)
            }

            R.id.btn_night -> {
                SkinCompatManager.getInstance().loadSkin("night", SkinCompatManager.SKIN_LOADER_STRATEGY_BUILD_IN)
                UIModeManager.getInstance().broadCastUiModeChanged(true)
                LiveDataBus.getDefault().postEvent("UI_MODE",true)
                AppDataStore.putData("UI_MODE",true)
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
                val result = MoshiUtil.fromJson<Result<List<BannerBean>>>(jsonString, type)

                Log.e("TAGTAG", "result--->$result")
            }

            R.id.btn_camera -> {
                CameraActivity.start(this@DemoActivity)
            }

            R.id.btn_deepseek -> {
                if (isFirst) {
                    isFirst = false;
                    ExecuteSSEUtil.getInstance()
                        .executeSSE(true, "你好，deepseek", object : IChatListener {
                            override fun onChatResult(
                                chatString: String?,
                                isEnd: Boolean,
                                isFirstPackage: Boolean
                            ) {

                            }

                            override fun onError(errorMsg: String?) {

                            }

                        })
                } else {
                    ExecuteSSEUtil.getInstance()
                        .executeSSE(true, "帮我介绍一下周传雄", object : IChatListener {
                            override fun onChatResult(
                                chatString: String?,
                                isEnd: Boolean,
                                isFirstPackage: Boolean
                            ) {

                            }

                            override fun onError(errorMsg: String?) {

                            }

                        })
                }

            }
            R.id.btn_browser ->{
                H5ContainerActivity.start(this@DemoActivity)
            }
            R.id.btn_keyboard->{
                KeyBoardDialog.getDialog().showAllowStateLoss(supportFragmentManager,"")
            }
        }
    }

    private var isFirst = true;
    override fun createViewModel(): DemoViewModel = DemoViewModel()
}