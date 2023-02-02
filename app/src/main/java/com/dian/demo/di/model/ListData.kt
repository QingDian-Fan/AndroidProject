package com.dian.demo.di.model

data class ListData<T>(
    val curPage: String? = null,
    val offset: String? = null,
    val over: String? = null,
    val pageCount: String? = null,
    val size: String? = null,
    val total: String? = null,
    val datas: ArrayList<T>? = null
)

data class ArticleBean(
    val id: String? = null,
    val link: String? = null,
    val title: String? = null,
    val superChapterName: String? = null
)