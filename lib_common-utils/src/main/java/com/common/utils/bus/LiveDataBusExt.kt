package com.common.utils.bus


import androidx.lifecycle.LiveData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// ---------- LiveData 扩展 ----------

/**
 * 直接返回类型安全的 LiveData<T?>，简化调用
 * LiveDataBus.getDefault().liveData<String>("LiveData").observe(this) { value ->
 *     println("LiveData 收到: $value")
 * }
 *
 */
fun <T> LiveDataBus.liveData(eventKey: Any, tag: String? = null, sticky: Boolean = false): LiveData<T?> {
    return this.subscribe<T>(eventKey, tag, sticky)
}

// ---------- Flow 扩展 ----------

/**
 * 将 LiveData 转成 Flow<T?>，支持协程收集
 *
 * 示例：
 * lifecycleScope.launch {
 *     LiveDataBus.getDefault().flow<String>("LiveData", sticky = true).collect {
 *         println("Flow 收到: $it")
 *     }
 * }
 */
fun <T> LiveDataBus.flow(eventKey: Any, tag: String? = null, sticky: Boolean = false): Flow<T?> {
    val liveData = this.subscribe<T>(eventKey, tag, sticky)
    return callbackFlow {
        val observer = androidx.lifecycle.Observer<T?> { value ->
            trySend(value)
        }
        liveData.observeForever(observer)
        awaitClose {
            liveData.removeObserver(observer)
        }
    }
}
