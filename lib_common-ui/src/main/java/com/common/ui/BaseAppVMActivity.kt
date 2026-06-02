package com.common.ui

import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.common.utils.ext.observeNonNull
import com.common.utils.ext.observeNullable

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/11 7:47 PM
 * @description: 集成 ViewModel 的 Activity 基类
 * @since: 1.0.0
 */
abstract class BaseAppVMActivity<B : ViewBinding, VM : BaseViewModel> :
    BaseAppBindActivity<B>() {

    protected lateinit var viewModel: VM
        private set

    override fun initContentView() {
        super.initContentView()
        viewModel = ViewModelProvider(this).get(getViewModelClass())
        lifecycle.addObserver(viewModel)
        observeViewModel()
    }

    protected abstract fun getViewModelClass(): Class<VM>

    private fun observeViewModel() {
        viewModel._loadingEvent.observeNonNull(this) { showLoadingView(it) }
        viewModel._emptyPageEvent.observeNonNull(this) { showEmptyView(it) }
        viewModel._errorPageEvent.observeNonNull(this) { showErrorView(it) }
        viewModel._toastEvent.observeNonNull(this) { showToast(it) }
        viewModel._pageNavigationEvent.observeNonNull(this) { navigate(it) }
        viewModel._backPressEvent.observeNullable(this) { backPress(it) }
        viewModel._finishPageEvent.observeNullable(this) { finishPage(it) }
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            lifecycle.removeObserver(viewModel)
        }
        super.onDestroy()
    }
}