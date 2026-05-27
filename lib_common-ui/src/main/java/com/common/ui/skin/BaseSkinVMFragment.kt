package com.common.ui.skin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import com.common.ui.BaseViewModel
import com.common.ui.ILazyLoad
import com.common.utils.ext.observeNonNull

abstract class BaseSkinVMFragment<B : ViewDataBinding, VM : BaseViewModel> :
    BaseSkinBindFragment<B>() {

    protected lateinit var viewModel: VM

    protected abstract fun getViewModelClass(): Class<VM>

    protected open fun isUseActivityViewModel(): Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setCurrentState(ILazyLoad.ON_CREATE_VIEW)
        if (getRootView() != null) return getRootView()
        injectDataBinding(inflater, container)
        return getRootView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        injectViewModel()
        initInternalObserver()
        initialize(savedInstanceState)
        excuteLazyInit(false)
        refreshSkinViews()
    }

    private fun injectViewModel() {
        viewModel = if (isUseActivityViewModel()) {
            ViewModelProvider(requireActivity())[getViewModelClass()]
        } else {
            ViewModelProvider(this)[getViewModelClass()]
        }
        viewModel.application = requireActivity().application
        lifecycle.addObserver(viewModel)
    }

    private fun initInternalObserver() {
        viewModel._loadingEvent.observeNonNull(viewLifecycleOwner) {
            showLoadingView(it)
        }
        viewModel._emptyPageEvent.observeNonNull(viewLifecycleOwner) {
            showEmptyView(it)
        }
        viewModel._errorPageEvent.observeNonNull(viewLifecycleOwner) {
            showErrorView(it)
        }
        viewModel._toastEvent.observeNonNull(viewLifecycleOwner) {
            showToast(it)
        }
        viewModel._pageNavigationEvent.observeNonNull(viewLifecycleOwner) {
            navigate(it)
        }
        viewModel._backPressEvent.observeNonNull(viewLifecycleOwner) {
            backPress(it)
        }
        viewModel._finishPageEvent.observeNonNull(viewLifecycleOwner) {
            finishPage(it)
        }
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            lifecycle.removeObserver(viewModel)
        }
        super.onDestroy()
    }
}
