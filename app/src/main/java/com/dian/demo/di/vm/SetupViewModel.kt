package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.SetUpData
import com.dian.demo.utils.LogUtil

class SetupViewModel: BaseViewModel() {

    val mSetUpData by lazy {
        MutableLiveData< MutableList<SetUpData>>()
    }

    val mNavigationData by lazy {
        MutableLiveData<MutableList<SetUpData>>()
    }

    fun getSetUpDataList(){
        launchOnUI {
            repo.getSetupDataList()
                .onSuccess {
                    mSetUpData.value = it?.toMutableList()
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

    fun getNavigationData(){
        launchOnUI {
            repo.getNavigationDataList()
                .onSuccess {
                    mNavigationData.value = it?.toMutableList()
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