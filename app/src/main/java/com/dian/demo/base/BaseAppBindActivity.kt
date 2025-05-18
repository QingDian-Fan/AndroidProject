package com.dian.demo.base

import android.view.LayoutInflater
import androidx.annotation.DrawableRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import com.demo.project.utils.ext.gone
import com.demo.project.utils.ext.visible
import com.dian.demo.R
import com.dian.demo.databinding.ActivityRootLayoutBinding
import com.dian.demo.ui.titlebar.CommonTitleBar


/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/12 10:06 AM
 * @description: 集成DataBinding的Activity的基类
 * @since: 1.0.0
 */
abstract class BaseAppBindActivity<B : ViewDataBinding> : BaseActivity() {

    protected lateinit var binding: B
    private lateinit var rootBinding: ActivityRootLayoutBinding

    override fun initContentView() {
        injectDataBinding()
        rootBinding.titleBar.setListener { _, action, _ ->
            if (action == CommonTitleBar.ACTION_LEFT_BUTTON) {
                onBackPressed()
            }
        }
    }

    private fun injectDataBinding() {
        rootBinding = DataBindingUtil.setContentView(this, R.layout.activity_root_layout)
        initRootView()
        rootBinding.lifecycleOwner = this
    }

    private fun initRootView() {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(this@BaseAppBindActivity),
            getLayoutId(),
            rootBinding.flRoot,
            false
        )
        rootBinding.flRoot.addView(binding.root)
    }

    fun setPageTitle(titleString:String){
        rootBinding.titleBar.setCenterText(titleString)
    }

    fun hideActionBack(){
        rootBinding.titleBar.setLeftVisibility(gone)
    }

    fun setPageRightIcon(@DrawableRes drawableRes: Int){
        rootBinding.titleBar.setRightIcon(drawableRes)
    }
    fun setPageLeftIcon(@DrawableRes drawableRes: Int){
        rootBinding.titleBar.setLeftIcon(drawableRes)
    }

    fun getTitleBarView():CommonTitleBar = rootBinding.titleBar


    override fun showLoadingView(isShow: Boolean) {
        if (isShow && !isFinishing) {
            rootBinding.flRoot.visibility = gone
            rootBinding.flEmpty.visibility = gone
            rootBinding.flLoading.visibility = visible
        } else if (!isFinishing) {
            rootBinding.flRoot.visibility = visible
            rootBinding.flEmpty.visibility = gone
            rootBinding.flLoading.visibility = gone
        }
    }

    override fun showEmptyView(isShow: Boolean) {
        if (isShow && !isFinishing) {
            rootBinding.flRoot.visibility = gone
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = visible
        } else if (!isFinishing) {
            rootBinding.flRoot.visibility = visible
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = gone
        }
    }

    override fun showErrorView(isShow: Boolean) {
        if (isShow && !isFinishing) {
            rootBinding.flRoot.visibility = gone
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = gone
            rootBinding.flError.visibility = visible
        } else if (!isFinishing) {
            rootBinding.flRoot.visibility = visible
            rootBinding.flLoading.visibility = gone
            rootBinding.flEmpty.visibility = gone
            rootBinding.flError.visibility = gone
        }
    }


    override fun onDestroy() {
        binding.unbind()
        super.onDestroy()
    }
}