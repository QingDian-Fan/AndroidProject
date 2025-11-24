package com.dian.demo.utils.webview.bean

/**
 * val result = list
 *     .associateBy { it.title to it.url } // 去重（忽略 timestamp）
 *     .values
 *     .sortedByDescending { it.timestamp } // 排序
 *
 */
data class WebDataEntry(
    val title: String? = "",
    val url: String? = "",
    val timestamp: Long
)


fun WebDataEntry.isSame(other: WebDataEntry): Boolean =
    this.title == other.title && this.url == other.url
