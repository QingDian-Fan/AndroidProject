package com.demo.project.base

import androidx.annotation.StringRes

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/9/3 5:23 下午
 * @description: Toast事件
 * @since: 1.0.0
 */
data class ToastEvent(
    var content: String? = null, // Toast内容字符串
    @StringRes var contentResId: Int? = null, // Toast内容字符串Id
    var showLong: Boolean = false // 是否长时间显示
)