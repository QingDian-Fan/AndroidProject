package com.demo.project.vm

import androidx.lifecycle.MutableLiveData
import com.common.ui.BaseViewModel
import kotlinx.coroutines.delay

class LoginViewModel : BaseViewModel() {

    val loginInfo by lazy { MutableLiveData<String>() }

    fun doLogin(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            showToast("账号或密码不能为空")
            return
        }
        launchOnUI {
            showLoadingView(true)
            delay(500)
            showLoadingView(false)
            loginInfo.value = username
        }
    }

    fun doRegister(username: String, password: String, rePassword: String) {
        if (username.isBlank() || password.isBlank()) {
            showToast("账号或密码不能为空")
            return
        }
        if (password != rePassword) {
            showToast("两次密码不一致")
            return
        }
        launchOnUI {
            showLoadingView(true)
            delay(500)
            showLoadingView(false)
            loginInfo.value = username
        }
    }
}