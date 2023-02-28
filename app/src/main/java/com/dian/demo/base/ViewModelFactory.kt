package com.dian.demo.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider



/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/9/3 5:23 下午
 * @description: 创建ViewModel的工厂，以此方法创建的ViewModel，可在构造函数中传参
 * @since: 1.0.0
 */
class ViewModelFactory(val viewModel: BaseViewModel) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return  viewModel as T
    }
}