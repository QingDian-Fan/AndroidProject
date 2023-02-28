package com.dian.demo.utils.ext

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/6/9 7:41 PM
 * @description: Obserser相关的扩展
 * @since: 1.0.0
 */

/**
 * 简化LiveData的订阅操作，参数可为null
 */
fun <T> LiveData<T>.observeNullable(owner: LifecycleOwner, block: (T) -> Unit) {
    this.observe(owner) {
        block(it)
    }
}

/**
 * 简化LiveData的订阅操作，参数不为null
 */
fun <T> LiveData<T>.observeNonNull(owner: LifecycleOwner, block: (T) -> Unit) {
    this.observe(owner) {
        if (it != null) {
            block(it)
        }
    }
}
