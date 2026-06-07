package com.common.ui

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.common.ui.databinding.ActivityRootLayoutBinding
import com.common.weight.titlebar.CommonTitleBar

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/12 10:06 AM
 * @description: 集成 ViewBinding 的 Activity 基类
 * @since: 1.0.0
 */
abstract class BaseAppBindActivity<B : ViewBinding> : BaseActivity() {

    protected enum class PageState { CONTENT, LOADING, EMPTY, ERROR }

    private var _rootBinding: ActivityRootLayoutBinding? = null
    private var _binding: B? = null
    protected val binding: B
        get() = _binding ?: error("binding accessed before initContentView() or after onDestroy()")

    protected val bindingOrNull: B?
        get() = _binding

    protected inline fun withBinding(block: B.() -> Unit) {
        bindingOrNull?.block()
    }
    /**
     * 子类返回页面对应的布局 id，基类会据此 inflate 并反射完成 ViewBinding 绑定。
     * 实现示例：`override fun getLayoutId() = R.layout.activity_xxx`
     */
    @LayoutRes
    abstract override fun getLayoutId(): Int

    override fun initContentView() {
        val root = ActivityRootLayoutBinding.inflate(layoutInflater)
        setContentView(root.root)
        val inner: B = ViewBindingReflect.bind(this, layoutInflater, root.flRoot, getLayoutId())
        root.flRoot.addView(inner.root)
        root.titleBar.setListener { _, action, _ ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressedDispatcher.onBackPressed()
            }
        }
        _rootBinding = root
        _binding = inner
    }

    fun setPageTitle(titleString: String) {
        _rootBinding?.titleBar?.setCenterText(titleString)
    }

    fun hideActionBack() {
        _rootBinding?.titleBar?.setLeftVisibility(View.GONE)
    }

    fun setPageRightIcon(@DrawableRes drawableRes: Int) {
        _rootBinding?.titleBar?.setRightIcon(drawableRes)
    }

    fun setPageLeftIcon(@DrawableRes drawableRes: Int) {
        _rootBinding?.titleBar?.setLeftIcon(drawableRes)
    }

    fun getTitleBarView(): CommonTitleBar? = _rootBinding?.titleBar

    protected fun setPageState(state: PageState) {
        val root = _rootBinding ?: return
        if (isFinishing) return
        root.flRoot.visibility = if (state == PageState.CONTENT) View.VISIBLE else View.GONE
        root.flLoading.visibility = if (state == PageState.LOADING) View.VISIBLE else View.GONE
        root.flEmpty.visibility = if (state == PageState.EMPTY) View.VISIBLE else View.GONE
        root.flError.visibility = if (state == PageState.ERROR) View.VISIBLE else View.GONE
    }

    override fun showLoadingView(isShow: Boolean) {
        setPageState(if (isShow) PageState.LOADING else PageState.CONTENT)
    }

    override fun showEmptyView(isShow: Boolean) {
        setPageState(if (isShow) PageState.EMPTY else PageState.CONTENT)
    }

    override fun showErrorView(isShow: Boolean) {
        setPageState(if (isShow) PageState.ERROR else PageState.CONTENT)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        _rootBinding = null
    }
}