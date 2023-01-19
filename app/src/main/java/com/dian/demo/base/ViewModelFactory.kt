package com.dian.demo.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.demo.project.base.BaseViewModel

/**
 * 创建ViewModel的工厂，以此方法创建的ViewModel，可在构造函数中传参
 */
class ViewModelFactory(val viewModel: BaseViewModel) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return  viewModel as T
    }
}