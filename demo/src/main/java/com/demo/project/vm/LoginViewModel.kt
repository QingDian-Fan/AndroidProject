package com.demo.project.vm

import androidx.lifecycle.MutableLiveData
import com.common.ui.BaseViewModel
import com.demo.project.R
import com.demo.project.model.LoginData
import com.demo.project.repository.remote.DataRepo
import com.demo.project.repository.remote.DataRepoImpl
import kotlinx.coroutines.delay

class LoginViewModel : BaseViewModel() {
    val loginInfo by lazy { MutableLiveData<LoginData>() }
    private val mRepo: DataRepo by lazy { DataRepoImpl() }
    fun doLogin(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            showToast(R.string.toast_account_password_empty)
            return
        }
        launchOnUI {
            mRepo.doLogin(username, password)
                .onSuccess {
                    loginInfo.value = it
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
            showToast(R.string.toast_account_password_empty)
            return
        }
        if (password != rePassword) {
            showToast(R.string.toast_password_not_match)
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
