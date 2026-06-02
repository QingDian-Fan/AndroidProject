package com.common.ui

import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/7 10:30 PM
 * @description: ViewModel 基类
 * @since: 1.0.0
 */
abstract class BaseViewModel : ViewModel(), DefaultLifecycleObserver, ViewBehavior {

    val _loadingEvent = MutableLiveData<Boolean>()
    val _emptyPageEvent = MutableLiveData<Boolean>()
    val _errorPageEvent = MutableLiveData<Boolean>()
    val _toastEvent = MutableLiveData<ToastEvent>()
    val _pageNavigationEvent = MutableLiveData<Any>()
    val _backPressEvent = MutableLiveData<Any?>()
    val _finishPageEvent = MutableLiveData<Any?>()

    protected fun launchOnUI(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main) { block() }
    }

    protected fun launchOnIO(block: suspend CoroutineScope.() -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO) { block() }
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
}
