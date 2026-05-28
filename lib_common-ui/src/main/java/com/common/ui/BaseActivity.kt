package com.common.ui

import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.activity.OnBackPressedCallback
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.common.utils.ToastUtil

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/20 12:08 PM
 * @description: Activity 基类
 * @since: 1.0.0
 */
abstract class BaseActivity : AppCompatActivity(), ViewBehavior {

    protected val TAG: String = this::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        super.onCreate(savedInstanceState)
        initContentView()
        supportActionBar?.hide()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!handleBackPress()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
        initialize(savedInstanceState)
    }

    protected open fun initContentView() {
        val layoutId = getLayoutId()
        if (layoutId != 0) setContentView(layoutId)
    }

    /**
     * 直接基于 layout id 的页面在这里返回布局；使用 ViewBinding 的子类无需重写。
     */
    @LayoutRes
    protected open fun getLayoutId(): Int = 0

    protected abstract fun initialize(savedInstanceState: Bundle?)

    /**
     * 子类返回 true 表示已自行消费返回键事件。
     */
    protected open fun handleBackPress(): Boolean = false

    protected fun showToast(text: String, showLong: Boolean = false) {
        showToast(ToastEvent(content = text, showLong = showLong))
    }

    protected fun showToast(@StringRes resId: Int, showLong: Boolean = false) {
        showToast(ToastEvent(contentResId = resId, showLong = showLong))
    }

    override fun showToast(event: ToastEvent) {
        event.content?.let {
            ToastUtil.showToast(this, it, event.showLong)
            return
        }
        if (event.contentResId != 0) {
            ToastUtil.showToast(this, getString(event.contentResId), event.showLong)
        }
    }

    override fun navigate(page: Any) {
        val targetPage = page as? Class<*> ?: return
        startActivity(Intent(this, targetPage))
    }

    override fun backPress(arg: Any?) {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun finishPage(arg: Any?) {
        finish()
    }
}