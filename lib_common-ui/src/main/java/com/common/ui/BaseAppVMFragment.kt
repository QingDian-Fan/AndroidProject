package com.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.common.utils.ext.observeNonNull

abstract class BaseAppVMFragment<B : ViewDataBinding, VM : BaseViewModel> :
    BaseAppBindFragment<B>() {

    protected lateinit var viewModel: VM

    //----------------------------------------------------------------------
    // ⭐ 1. 使用 VM Class，而不是返回实例
    //----------------------------------------------------------------------
    protected abstract fun getViewModelClass(): Class<VM>

    //----------------------------------------------------------------------
    // ⭐ 2. 是否使用 Activity 级别的 VM
    //----------------------------------------------------------------------
    protected open fun isUseActivityViewModel(): Boolean = false

    //----------------------------------------------------------------------
    // ⭐ 3. 视图创建 —— 只负责创建 View，不初始化 ViewModel
    //----------------------------------------------------------------------
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

    //----------------------------------------------------------------------
    // ⭐ 4. 真正初始化 ViewModel + Observer —— 放在 onViewCreated
    //----------------------------------------------------------------------
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        injectViewModel()
        initInternalObserver()
        initialize(savedInstanceState)
        excuteLazyInit(false)
    }

    private fun injectViewModel() {
        viewModel = if (isUseActivityViewModel()) {
            ViewModelProvider(requireActivity()).get(getViewModelClass())
        } else {
            ViewModelProvider(this).get(getViewModelClass())
        }

        // 统一注入 Application
        viewModel.application = requireContext().applicationContext as android.app.Application

        // lifecycleObserver 安全添加
        lifecycle.addObserver(viewModel)
    }

    //----------------------------------------------------------------------
    // ⭐ 5. 所有 LiveData observer 必须使用 viewLifecycleOwner
    //----------------------------------------------------------------------
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

    //----------------------------------------------------------------------
    // ⭐ 6. 清理
    //----------------------------------------------------------------------
    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }
}
