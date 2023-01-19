package com.dian.demo.base

import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.demo.project.base.BaseViewModel
import com.dian.demo.utils.ext.observeNonNull
import com.dian.demo.utils.ext.observeNullable


/**
 * @author: Albert Li
 * @contact: albertlii@163.com
 * @time: 2020/6/11 7:47 PM
 * @description: --
 * @since: 1.0.0
 */
abstract class BaseAppVMActivity<B : ViewDataBinding, VM : BaseViewModel> : BaseAppBindActivity<B>() {
    protected lateinit var viewModel: VM

    override fun initContentView() {
        super.initContentView()
        injectViewModel()
        initInternalObserver()
    }

    private fun injectViewModel() {
        val vm = createViewModel()
        viewModel = ViewModelProvider(this, BaseViewModel.createViewModelFactory(vm))
            .get(vm::class.java)
        viewModel.application = application
        lifecycle.addObserver(viewModel)
    }

    protected abstract fun createViewModel(): VM

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
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }

    override fun showEmptyView(isShow: Boolean) {

    }


}