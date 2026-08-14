package com.demo.project.vm

import androidx.lifecycle.MutableLiveData
import com.common.aop.CheckNet
import com.common.ui.BaseViewModel
import com.demo.project.R
import com.demo.project.model.LoginData
import com.demo.project.repository.remote.DataRepo
import com.demo.project.repository.remote.DataRepoImpl
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart

class LoginViewModel : BaseViewModel() {
    val loginInfo by lazy { MutableLiveData<LoginData>() }
    private val mRepo: DataRepo by lazy { DataRepoImpl() }
    @CheckNet
    fun doLogin(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            showToast(R.string.toast_account_password_empty)
            return
        }
        launchOnUI {
            showLoadingView(true)
            mRepo.doLogin(username, password)
                .onSuccess {
                    showLoadingView(false)
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

    @CheckNet
    fun doRegister(username: String, password: String, rePassword: String) {
        if (username.isBlank() || password.isBlank() || rePassword.isBlank()) {
            showToast(R.string.toast_account_password_empty)
            return
        }
        if (password != rePassword) {
            showToast(R.string.toast_password_not_match)
            return
        }
        launchOnUI {
            mRepo.doRegister(username, password, rePassword)
                .onStart { showLoadingView(true) }
                .onCompletion { showLoadingView(false) }
                .collect { response ->
                    response
                        .onSuccess {
                            loginInfo.value = it
                        }
                        .onFailure { _, message ->
                            if (message.isNullOrBlank()) {
                                showErrorView(true)
                            } else {
                                showToast(message)
                            }
                        }
                        .onCatch {
                            showErrorView(true)
                        }
                }
        }
    }
}
