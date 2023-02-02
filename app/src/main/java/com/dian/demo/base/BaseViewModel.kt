package com.dian.demo.base

import android.annotation.SuppressLint
import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import androidx.lifecycle.ViewModel
import com.demo.project.base.ToastEvent
import com.demo.project.base.ViewBehavior
import com.dian.demo.ProjectApplication
import com.dian.demo.di.repository.remote.DataRepoImpl
import com.dian.demo.di.repository.local.DataBaseManager
import kotlinx.coroutines.launch

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/7 10:30 PM
 * @description: ViewModel的基类
 * @since: 1.0.0
 */
abstract class BaseViewModel : ViewModel(), ViewModelLifecycle, ViewBehavior {
    protected val repo by lazy { DataRepoImpl() }
    protected val localRepo by lazy { DataBaseManager }

    // loading视图显示Event
    var _loadingEvent = MutableLiveData<Boolean>()

    // 无数据视图显示Event
    var _emptyPageEvent = MutableLiveData<Boolean>()

    // 无数据视图显示Event
    var _errorPageEvent = MutableLiveData<Boolean>()

    // toast提示Event
    var _toastEvent = MutableLiveData<ToastEvent>()

    // 不带参数的页面跳转Event
    var _pageNavigationEvent = MutableLiveData<Any>()

    // 点击系统返回键Event
    var _backPressEvent = MutableLiveData<Any?>()

    // 关闭页面Event
    var _finishPageEvent = MutableLiveData<Any?>()


    @SuppressLint("StaticFieldLeak")
    lateinit var application: Application

    private lateinit var lifcycleOwner: LifecycleOwner

    /**
     * 在主线程中执行一个协程
     */
    protected fun launchOnUI(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main) { block() }
    }

    /**
     * 在IO线程中执行一个协程
     */
    protected fun launchOnIO(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO) { block() }
    }

    override fun onAny(owner: LifecycleOwner, event: Lifecycle.Event) {
        this.lifcycleOwner = owner
    }

    override fun onCreate() {

    }

    override fun onStart() {

    }

    override fun onResume() {

    }

    override fun onPause() {

    }

    override fun onStop() {

    }

    override fun onDestroy() {

    }

    override fun showLoadingView(isShow: Boolean) {
        _loadingEvent.postValue(isShow)
    }

    override fun showEmptyView(isShow: Boolean) {
        _emptyPageEvent.postValue(isShow)
    }

    override fun showErrorView(isShow: Boolean) {
        _errorPageEvent.postValue(isShow)
    }

    override fun showToast(event: ToastEvent) {
        _toastEvent.postValue(event)
    }

    protected fun showToast(text: String, showLong: Boolean = false) {
        showToast(ToastEvent(content = text, showLong = showLong))
    }

    protected fun showToast(@StringRes resId: Int, showLong: Boolean = false) {
        showToast(ToastEvent(contentResId = resId, showLong = showLong))
    }

    override fun navigate(page: Any) {
        _pageNavigationEvent.postValue(page)
    }

    override fun backPress(arg: Any?) {
        _backPressEvent.postValue(arg)
    }

    protected fun backPress() {
        backPress(null)
    }

    override fun finishPage(arg: Any?) {
        _finishPageEvent.postValue(arg)
    }

    protected fun finishPage() {
        finishPage(null)
    }

    companion object {
        @JvmStatic
        fun <T : BaseViewModel> createViewModelFactory(viewModel: T): ViewModelProvider.Factory {
            return ViewModelFactory(viewModel)
        }
    }
}



