package com.dian.demo.di.vm

import androidx.lifecycle.MutableLiveData
import com.dian.demo.base.BaseViewModel
import com.dian.demo.di.model.CoinCount
import com.dian.demo.utils.LogUtil

class CoinRankViewModel : BaseViewModel() {
    val coinRankData by lazy {
        MutableLiveData<List<CoinCount>>()
    }
    fun getCoinRankList(mPage:Int){
        launchOnUI {
            repo.getCoinRankList(mPage)
                .onCompletion {
                    LogUtil.e("TAG----->Request", "articleList请求结束")
                    showLoadingView(false)
                }
                .onSuccess { it ->
                    it?.datas?.let {
                        coinRankData.value = it
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