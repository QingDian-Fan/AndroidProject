package com.demo.project.vm

import androidx.lifecycle.MutableLiveData
import com.common.ui.BaseViewModel
import com.demo.project.repository.remote.DataRepo
import kotlinx.coroutines.delay

class LoginViewModel : BaseViewModel() {
    private val repo by repo<DataRepo>()
  //  protected val localRepo by lazy { DataBaseManager }
    val loginInfo by lazy { MutableLiveData<String>() }

    fun doLogin(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            showToast("账号或密码不能为空")
            return
        }
        launchOnUI {
            repo.doLogin(username, password)
                .onSuccess {
                    //loginInfo.value = it
                }
                .onFailure { _, _ ->
                    showErrorView(true)
                }
                .onCatch {
                    showErrorView(true)
                }
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