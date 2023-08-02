package com.dian.demo.di.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.utils.LogUtil

class SettingViewModel:BaseViewModel() {
     val isLoginOut = MutableLiveData<Boolean>()
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