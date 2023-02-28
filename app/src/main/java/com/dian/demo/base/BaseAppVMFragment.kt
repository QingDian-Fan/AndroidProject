package com.dian.demo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.demo.project.base.ILazyLoad
import com.dian.demo.utils.ext.observeNonNull

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/9/3 4:18 下午
 * @description: -
 * @since: 1.0.0
 */
abstract class BaseAppVMFragment<B : ViewDataBinding, VM : BaseViewModel> : BaseAppBindFragment<B>() {
    protected lateinit var viewModel: VM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setCurrentState(ILazyLoad.ON_CREATE_VIEW)
        if (getRootView() != null) return getRootView()
        injectDataBinding(inflater, container)
        injectViewModel()
        initialize(savedInstanceState)
        initInternalObserver()
        excuteLazyInit(false)
        return getRootView()
    }



    private fun injectViewModel() {
        val vm = createViewModel()
        viewModel = ViewModelProvider(this, BaseViewModel.createViewModelFactory(vm))
            .get(vm::class.java)
        viewModel.application = requireActivity().application
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
        viewModel._backPressEvent.observeNonNull(this) {
            backPress(it)
        }
        viewModel._finishPageEvent.observeNonNull(this) {
            finishPage(it)
        }
    }

    protected abstract fun createViewModel(): VM;

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        binding.unbind()
        super.onDestroy()
    }

}