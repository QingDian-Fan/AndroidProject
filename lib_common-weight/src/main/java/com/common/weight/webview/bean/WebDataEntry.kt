package com.common.weight.webview.bean

/**
 * 浏览历史、书签、收藏共用的网页记录。
 */
data class WebDataEntry(
    val title: String? = "",
    val url: String? = "",
    val timestamp: Long
)

/** 按 (title,url) 判定两条记录是否表示同一个网页。 */
fun WebDataEntry.isSame(other: WebDataEntry): Boolean =
    this.title == other.title && this.url == other.url
