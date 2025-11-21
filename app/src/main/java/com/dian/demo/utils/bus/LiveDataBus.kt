package com.dian.demo.utils.bus

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentHashMap

/**
 * 类型安全事件总线，支持 Sticky 事件和可空值
 *
 * 使用示例:
 *
 * LiveDataBus.getDefault().postEvent("LiveData", "hi LiveData")
 *
 * LiveDataBus.getDefault().subscribe<String>("LiveData").observe(this) { value ->
 *     println("onChanged: $value")
 * }
 *
 * Sticky 示例:
 *
 * LiveDataBus.getDefault().subscribe<String>("LiveData", sticky = true).observe(this) { value ->
 *     println("sticky onChanged: $value")
 * }
 */
class LiveDataBus private constructor() {

    private val mLiveDataBus: ConcurrentHashMap<String, LiveBusData<*>> = ConcurrentHashMap()

    companion object {
        @Volatile
        private var instance: LiveDataBus? = null

        fun getDefault(): LiveDataBus {
            if (instance == null) {
                synchronized(LiveDataBus::class.java) {
                    if (instance == null) {
                        instance = LiveDataBus()
                    }
                }
            }
            return instance!!
        }
    }

    /**
     * 订阅事件
     * @param sticky 是否立即收到最近一次发送的值
     */
    fun <T> subscribe(eventKey: Any, tag: String? = null, sticky: Boolean = false): MutableLiveData<T?> {
        val key = mergeEventKey(eventKey, tag)
        val liveData = mLiveDataBus.getOrPut(key) { LiveBusData<T?>(true) }

        if (liveData is LiveBusData<*>) {
            (liveData as LiveBusData<T?>).apply {
                isFirstSubscribe = false
                if (sticky) isSticky = true
            }
        }

        @Suppress("UNCHECKED_CAST")
        return liveData as MutableLiveData<T?>
    }
    /**
     * 发送事件
     */
    fun <T> postEvent(eventKey: Any,  value: T?){
        this.postEvent(eventKey = eventKey, tag = null,value = value)
    }

    fun <T> postEvent(eventKey: Any, tag: String? = null, value: T?) {
        val liveData = subscribe<T?>(eventKey, tag)
        if (liveData is LiveBusData<*>) {
            (liveData as LiveBusData<T?>).lastValue = value
        }
        liveData.postValue(value)
    }

    class LiveBusData<T>(var isFirstSubscribe: Boolean) : MutableLiveData<T>() {
        var lastValue: T? = null
        var isSticky: Boolean = false

        override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
            // 使用匿名类包装 Observer，避免访问 AndroidX 内部类
            val wrapper = object : Observer<T> {
                var isChanged = isFirstSubscribe
                override fun onChanged(value: T) {
                    if (isSticky && lastValue != null && !isChanged) {
                        observer.onChanged(lastValue!!)
                        isChanged = true
                        return
                    }
                    if (isChanged) {
                        observer.onChanged(value)
                    } else {
                        isChanged = true
                    }
                }
            }
            super.observe(owner, wrapper)
        }
    }

    private fun mergeEventKey(eventKey: Any, tag: String?): String {
        return if (!tag.isNullOrEmpty()) "$eventKey$tag" else eventKey.toString()
    }

    fun clear(eventKey: Any, tag: String? = null) {
        val key = mergeEventKey(eventKey, tag)
        mLiveDataBus.remove(key)
    }
}