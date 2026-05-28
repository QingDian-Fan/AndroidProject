package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.UserInfo
import com.project.common.utils.LogUtil

class SettingViewModel:BaseViewModel() {
     val isLoginOut = MutableLiveData<Boolean>()
     val userData = MutableLiveData<UserInfo>()

    fun getUserInfo(){
        launchOnUI {
            repo.getUserInfo()
                .onSuccess {
                    userData.value = it
                }
        }
    }
    fun doLoginOut(){
        launchOnUI {
            repo.doLoginOut()
                .onSuccess {
                    isLoginOut.value = true
                }
                .onCompletion {

                }
                .onFailure { code, message ->
                    LogUtil.e("TAG----->","$code");
                }
                .onCatch {

                }

        }
    }
}