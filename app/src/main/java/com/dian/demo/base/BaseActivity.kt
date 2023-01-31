package com.dian.demo.base

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import com.demo.project.base.ToastEvent
import com.demo.project.base.ViewBehavior
import com.dian.demo.ProjectApplication
import com.dian.demo.utils.ScreenShotListenManager
import com.dian.demo.utils.ToastUtil
import com.dian.demo.utils.share.ShareActivity


/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/20 12:08 PM
 * @description: Activity的基类
 * @since: 1.0.0
 */
abstract class BaseActivity : ShareActivity(), ViewBehavior {

    protected val TAG = "${this.javaClass.simpleName}----->"
    protected val manager = ScreenShotListenManager(ProjectApplication.getAppContext())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        initContentView()
        supportActionBar?.hide()
        initialize(savedInstanceState)

    }

    protected open fun initContentView() {
        setContentView(getLayoutId())
    }

    @LayoutRes
    protected abstract fun getLayoutId(): Int

    /**
     *  初始化操作
     */
    protected abstract fun initialize(savedInstanceState: Bundle?)

    protected fun showToast(text: String, showLong: Boolean = false) {
        showToast(ToastEvent(content = text, showLong = showLong))
    }

    protected fun showToast(@StringRes resId: Int, showLong: Boolean = false) {
        showToast(ToastEvent(contentResId = resId, showLong = showLong))
    }

    override fun showToast(event: ToastEvent) {
        if (event.content != null) {
            ToastUtil.showToast(this, event.content!!, event.showLong)
        } else if (event.contentResId != null) {
            ToastUtil.showToast(this, getString(event.contentResId!!), event.showLong)
        }
    }

    override fun navigate(page: Any) {
        startActivity(Intent(this, page as Class<*>))
    }

    override fun backPress(arg: Any?) {
        onBackPressed()
    }

    override fun finishPage(arg: Any?) {
        finish()
    }


    override fun onResume() {
        super.onResume()
        startListener()
    }

    override fun onPause() {
        super.onPause()
        stopListener()
    }

    //截屏监听
    private fun startListener() {
        manager.setListener { imagePath ->

        }
        manager.startListen()
    }

    private fun stopListener() {
        manager.stopListen()
    }

}