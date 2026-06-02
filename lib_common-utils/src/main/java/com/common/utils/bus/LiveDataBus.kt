package com.common.utils.bus

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger


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
        val liveData = mLiveDataBus.getOrPut(key) { LiveBusData<T?>(sticky = sticky) }

        if (liveData is LiveBusData<*>) {
            @Suppress("UNCHECKED_CAST")
            val l = liveData as LiveBusData<T?>
            l.isSticky = sticky
            return l
        }
        @Suppress("UNCHECKED_CAST")
        return liveData as MutableLiveData<T?>
    }

    /**
     * 发送事件（异步）
     */
    fun <T> postEvent(eventKey: Any, value: T?) {
        this.postEvent(eventKey = eventKey, tag = null, value = value)
    }
    @Suppress("UNCHECKED_CAST")
    fun <T> postEvent(eventKey: Any, tag: String? = null, value: T?) {
        val key = mergeEventKey(eventKey, tag)
        val liveData = mLiveDataBus.getOrPut(key) { LiveBusData<T?>(sticky = false) } as LiveBusData<T?>
        liveData.postPublish(value)
    }

    /**
     * 清理事件通道
     */
    fun clear(eventKey: Any, tag: String? = null) {
        val key = mergeEventKey(eventKey, tag)
        mLiveDataBus.remove(key)
    }

    private fun mergeEventKey(eventKey: Any, tag: String?): String {
        return if (!tag.isNullOrEmpty()) "$eventKey$tag" else eventKey.toString()
    }

    /**
     * 内部 LiveData 类型
     */
    class LiveBusData<T>(sticky: Boolean = false) : MutableLiveData<T>() {

        @Volatile
        var lastValue: T? = null

        @Volatile
        var isSticky: Boolean = sticky

        // 版本号，每次 postEvent/setValue 时递增
        private val versionCounter = AtomicInteger(0)

        /** 异步发布事件 */
        fun postPublish(value: T?) {
            lastValue = value
            versionCounter.incrementAndGet()
            super.postValue(value)
        }

        /** 同步发布事件 */
        fun setPublish(value: T?) {
            lastValue = value
            versionCounter.incrementAndGet()
            super.setValue(value)
        }

        override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {

            val startVersion = versionCounter.get()
            val wrapper = object : Observer<T> {
                var lastSeenVersion = startVersion
                var stickyDelivered = false

                override fun onChanged(value: T) {
                    val currentVersion = versionCounter.get()

                    // sticky 回放一次 lastValue
                    if (isSticky && !stickyDelivered && lastValue != null) {
                        stickyDelivered = true
                        lastSeenVersion = currentVersion
                        @Suppress("UNCHECKED_CAST")
                        observer.onChanged(lastValue as T)
                        return
                    }

                    // 只派发新版本
                    if (currentVersion > lastSeenVersion) {
                        lastSeenVersion = currentVersion
                        observer.onChanged(value)
                    }
                }
            }
            super.observe(owner, wrapper)
        }

    }
}
