package com.dian.demo.di.vm

import com.dian.demo.base.BaseViewModel
import com.dian.demo.utils.aop.CheckNet

class TodoViewModel : BaseViewModel() {

    @CheckNet
    fun getTodoList(page: Int) {
        launchOnUI {
            repo.getTodoList(page)
                .onCompletion {
                    showLoadingView(false)
                }
                .onSuccess {
                    showToast("请求成功")
                }
                .onFailure { _, _ ->
                    showErrorView(true)
                }
                .onCatch {
                    showErrorView(true)
                }
        }
    }
}