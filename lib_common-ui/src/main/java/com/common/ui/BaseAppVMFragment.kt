package com.common.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.common.utils.ext.observeNonNull
import com.common.utils.ext.observeNullable

/**
 * 集成 ViewModel 的 Fragment 基类。
 */
abstract class BaseAppVMFragment<B : ViewBinding, VM : BaseViewModel> :
    BaseAppBindFragment<B>() {

    protected lateinit var viewModel: VM
        private set

    protected abstract fun getViewModelClass(): Class<VM>

    protected open fun isUseActivityViewModel(): Boolean = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val owner = if (isUseActivityViewModel()) requireActivity() else this
        viewModel = ViewModelProvider(owner).get(getViewModelClass())
        lifecycle.addObserver(viewModel)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel._loadingEvent.observeNonNull(viewLifecycleOwner) { showLoadingView(it) }
        viewModel._emptyPageEvent.observeNonNull(viewLifecycleOwner) { showEmptyView(it) }
        viewModel._errorPageEvent.observeNonNull(viewLifecycleOwner) { showErrorView(it) }
        viewModel._toastEvent.observeNonNull(viewLifecycleOwner) { showToast(it) }
        viewModel._pageNavigationEvent.observeNonNull(viewLifecycleOwner) { navigate(it) }
        viewModel._backPressEvent.observeNullable(viewLifecycleOwner) { backPress(it) }
        viewModel._finishPageEvent.observeNullable(viewLifecycleOwner) { finishPage(it) }
    }

    override fun onDestroy() {
        if (::viewModel.isInitialized) {
            lifecycle.removeObserver(viewModel)
        }
        super.onDestroy()
    }
}