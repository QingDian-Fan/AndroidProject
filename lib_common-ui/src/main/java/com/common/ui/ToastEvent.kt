package com.common.ui

import androidx.annotation.StringRes

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/9/3 5:23 下午
 * @description: Toast 事件
 * @since: 1.0.0
 */
data class ToastEvent(
    val content: String? = null,
    @StringRes val contentResId: Int = 0,
    val showLong: Boolean = false
)