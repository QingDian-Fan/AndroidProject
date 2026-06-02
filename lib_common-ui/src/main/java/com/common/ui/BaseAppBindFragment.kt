package com.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import com.common.ui.databinding.FragmentRootLayoutBinding
import com.common.weight.titlebar.CommonTitleBar

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/9/3 6:19 下午
 * @description: 集成 ViewBinding 的 Fragment 基类
 * @since: 1.0.0
 */
abstract class BaseAppBindFragment<B : ViewBinding> : BaseFragment() {

    protected enum class PageState { CONTENT, LOADING, EMPTY, ERROR }

    private var _rootBinding: FragmentRootLayoutBinding? = null
    private var _binding: B? = null
    protected val binding: B
        get() = _binding ?: error("binding accessed before onCreateView() or after onDestroyView()")

    /**
     * 子类返回页面对应的布局 id，基类会据此 inflate 并反射完成 ViewBinding 绑定。
     * 实现示例：`override fun getLayoutId() = R.layout.fragment_xxx`
     */
    @LayoutRes
    abstract override fun getLayoutId(): Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = FragmentRootLayoutBinding.inflate(inflater, container, false)
        val inner: B = ViewBindingReflect.bind(this, inflater, root.flRoot, getLayoutId())
        root.flRoot.addView(inner.root)
        _rootBinding = root
        _binding = inner
        return root.root
    }

    override fun onDestroyView() {
        _binding = null
        _rootBinding = null
        super.onDestroyView()
    }

    fun setPageTitle(titleString: String) {
        (activity as? BaseAppBindActivity<*>)?.setPageTitle(titleString)
    }

    fun getTitleBarView(): CommonTitleBar? =
        (activity as? BaseAppBindActivity<*>)?.getTitleBarView()

    protected fun setPageState(state: PageState) {
        val root = _rootBinding ?: return
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
}