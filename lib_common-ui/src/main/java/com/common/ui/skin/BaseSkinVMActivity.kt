package com.common.ui.skin

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.common.ui.BaseViewModel
import com.common.utils.ext.observeNonNull
import com.common.utils.ext.observeNullable

abstract class BaseSkinVMActivity<B : ViewDataBinding, VM : BaseViewModel> :
    BaseSkinBindActivity<B>() {

    protected lateinit var viewModel: VM

    override fun initContentView() {
        super.initContentView()
        injectViewModel()
        initInternalObserver()
    }

    protected abstract fun createViewModel(): VM

    private fun injectViewModel() {
        val vm = createViewModel()
        viewModel = ViewModelProvider(this, BaseViewModel.createViewModelFactory(vm))[vm::class.java]
        viewModel.application = application
        lifecycle.addObserver(viewModel)
    }

    private fun initInternalObserver() {
        viewModel._loadingEvent.observeNonNull(this) {
            showLoadingView(it)
        }
        viewModel._emptyPageEvent.observeNonNull(this) {
            showEmptyView(it)
        }
        viewModel._errorPageEvent.observeNonNull(this) {
            showErrorView(it)
        }
        viewModel._toastEvent.observeNonNull(this) {
            showToast(it)
        }
        viewModel._pageNavigationEvent.observeNonNull(this) {
            navigate(it)
        }
        viewModel._backPressEvent.observeNullable(this) {
            backPress(it)
        }
        viewModel._finishPageEvent.observeNullable(this) {
            finishPage(it)
        }
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            lifecycle.removeObserver(viewModel)
        }
        super.onDestroy()
    }

    override fun showEmptyView(isShow: Boolean) {
    }
}
