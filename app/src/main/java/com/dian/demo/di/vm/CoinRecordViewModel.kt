package com.dian.demo.di.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.CoinCount
import com.dian.demo.utils.LogUtil

class CoinRecordViewModel: BaseViewModel() {
    val coinRecordData by lazy {
        MutableLiveData<List<CoinCount>>()
    }
    fun getCoinRecordList(page: Int){
        launchOnUI {
            repo.getCoinRecordList(page)
                .onCompletion {
                    LogUtil.e("TAG----->Request", "articleList请求结束")
                    showLoadingView(false)
                }
                .onSuccess { it ->
                    it?.datas?.let {
                        coinRecordData.value = it
                    }
                }
                .onFailure { code, message ->
                    LogUtil.e("TAG----->Request", "articleList失败：code：${code},message:${message}")
                    showErrorView(true)
                }
                .onCatch {
                    LogUtil.e(
                        "TAG----->Request",
                        "articleList失败：code：${it.errorCode},message:${it.errorMsg}"
                    )
                    showErrorView(true)
                }
        }
    }
}