package com.common.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.common.utils.ToastUtil

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/19 9:42 PM
 * @description: Fragment 基类
 * @since: 1.0.0
 */
abstract class BaseFragment : Fragment(), ViewBehavior {

    protected val TAG: String = this::class.java.simpleName

    private var hasLazyInitialized = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = getLayoutId()
        return if (layoutId != 0) inflater.inflate(layoutId, container, false) else null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initialize(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        tryLazyInit()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) tryLazyInit()
    }

    override fun onDestroyView() {
        hasLazyInitialized = false
        super.onDestroyView()
    }

    private fun tryLazyInit() {
        if (hasLazyInitialized) return
        if (isHidden) return
        if (view == null) return
        hasLazyInitialized = true
        lazyInit()
    }

    /**
     * 懒加载入口。
     *
     * 调用时机：
     * - ViewPager2 + FragmentStateAdapter：被滑到当前页（maxLifecycle 升到 RESUMED）时；
     * - replace/add 单页：onResume 时；
     * - add+show+hide：从 hide → show 时（onHiddenChanged(false)）。
     *
     * 每次 view 重新创建（onDestroyView 后）会重新触发一次，便于 ViewPager2 在远端缓存被回收
     * 重新回到该页时刷新数据。如果想跨 view 持久化，把数据放到 ViewModel 即可。
     */
    protected open fun lazyInit() {}

    /**
     * 直接基于 layout id 的页面在这里返回布局；使用 ViewBinding 的子类无需重写。
     */
    @LayoutRes protected open fun getLayoutId(): Int = 0

    protected abstract fun initialize(savedInstanceState: Bundle?)

    protected fun showToast(text: String, showLong: Boolean = false) {
        showToast(ToastEvent(content = text, showLong = showLong))
    }

    protected fun showToast(@StringRes resId: Int, showLong: Boolean = false) {
        showToast(ToastEvent(contentResId = resId, showLong = showLong))
    }

    override fun showToast(event: ToastEvent) {
        val safeContext = context ?: return
        event.content?.let {
            ToastUtil.showToast(safeContext, it, event.showLong)
            return
        }
        if (event.contentResId != 0) {
            ToastUtil.showToast(safeContext, safeContext.getString(event.contentResId), event.showLong)
        }
    }

    override fun navigate(page: Any) {
        val safeContext = context ?: return
        val targetPage = page as? Class<*> ?: return
        startActivity(Intent(safeContext, targetPage))
    }

    override fun backPress(arg: Any?) {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    override fun finishPage(arg: Any?) {
        activity?.finish()
    }
}