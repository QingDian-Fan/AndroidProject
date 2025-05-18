package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.ArticleEntity
import com.dian.demo.di.model.UserBean

class LoginViewModel : BaseViewModel() {

    val loginInfo by lazy {
        MutableLiveData<UserBean>()
    }

    fun doLogin(username: String, password: String) {
        launchOnUI {
            repo.doLogin(username, password)
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

    fun doRegister(username: String, password: String, repassword: String) {
        launchOnUI {
            repo.doRegister(username, password, repassword)
                .onSuccess {
                    loginInfo.value = it
                }
                .onFailure { code, message ->

                }
                .onCatch {

                }
        }
    }

}