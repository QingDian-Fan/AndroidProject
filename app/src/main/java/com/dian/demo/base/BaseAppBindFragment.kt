package com.dian.demo.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.demo.project.base.ILazyLoad
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.databinding.FragmentRootLayoutBinding

/**
 * @author: Albert Li
 * @contact: albertlii@163.com
 * @time: 2021/9/3 6:19 下午
 * @description: -
 * @since: 1.0.0
 */
abstract class BaseAppBindFragment<B : ViewDataBinding> : BaseFragment() {
    protected lateinit var binding: B
    private lateinit var rootBinding: FragmentRootLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setCurrentState(ILazyLoad.ON_CREATE_VIEW)
        if (getRootView() != null) return getRootView()
        injectDataBinding(inflater, container)
        initialize(savedInstanceState)
        excuteLazyInit(false)
        return getRootView()
    }

    protected open fun injectDataBinding(inflater: LayoutInflater, container: ViewGroup?) {
        rootBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_root_layout, container, false)
        binding = DataBindingUtil.inflate(inflater, getLayoutId(), rootBinding.flRoot, false)
        rootBinding.flRoot.addView(binding.root)
        rootBinding.lifecycleOwner = this
        setRootView(rootBinding.root)
    }

    override fun onDestroyView() {
        binding.unbind()
        super.onDestroyView()
    }
    fun setPageTitle(titleString:String){
        (activity as BaseAppBindActivity<*>).setPageTitle(titleString)
    }
    override fun showLoadingView(isShow: Boolean) {
        if (isShow) {
            rootBinding.flRoot.visibility = gone
            rootBinding.flEmpty.visibility = gone
            rootBinding.flLoading.visibility = visible
        } else {
            rootBinding.flRoot.visibility = visible
            rootBinding.flEmpty.visibility = gone
            rootBinding.flLoading.visibility = gone
        }
    }

    override fun showEmptyView(isShow: Boolean) {
        if (isShow) {
            rootBinding.flRoot.visibility = gone
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = visible
        } else {
            rootBinding.flRoot.visibility = visible
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = gone
        }
    }

    override fun showErrorView(isShow: Boolean) {
        if (isShow) {
            rootBinding.flRoot.visibility = gone
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = gone
            rootBinding.flError.visibility = visible
        } else {
            rootBinding.flRoot.visibility = visible
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = gone
            rootBinding.flError.visibility = gone
        }
    }

}