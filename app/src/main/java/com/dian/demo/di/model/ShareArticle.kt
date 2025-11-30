package com.dian.demo.di.model

data class ShareArticle(
    var coinInfo: CoinInfo? = null,
    var shareArticles: ListData<ArticleBean>
    )