package com.demo.project.utils

import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * 此观察者 去 观察 BaseActivity 的 生命周期方法
 */
object CustomNetworkStateManager : DefaultLifecycleObserver {

    // 如果当网络状态发生变化时，让BaseFragment --  TODO 子类可以重写该方法，统一的网络状态通知和处理
    // val networkStateCallback = CustomProjectLiveData<NetworkState>()

    private var networkStateReceiver: NetworkStateReceiver? = null

    @JvmStatic
    fun getInstance(): CustomNetworkStateManager = this

    /**
     * 那么观察到 观察 BaseActivity 的 生命周期方法 后 做什么事情呢？
     * 答；就是注册一个 广播，此广播可以接收到信息（然后 输出 "网络不给力"）
     */
    override fun onResume(owner: LifecycleOwner) {
        try {
            val receiver = NetworkStateReceiver().also { networkStateReceiver = it }
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            when (owner) {
                is AppCompatActivity -> owner.registerReceiver(receiver, filter)
                is Fragment -> owner.requireActivity().registerReceiver(receiver, filter)
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    /**
     * 那么观察到 观察 BaseActivity 的 生命周期方法 后 做什么事情呢？
     * 答；就是移除一个 广播
     */
    override fun onPause(owner: LifecycleOwner) {
        try {
            when (owner) {
                is AppCompatActivity -> owner.unregisterReceiver(networkStateReceiver)
                is Fragment -> owner.requireActivity().unregisterReceiver(networkStateReceiver)
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}