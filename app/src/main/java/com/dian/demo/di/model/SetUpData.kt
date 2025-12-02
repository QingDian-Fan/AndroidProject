package com.dian.demo.di.model

data class SetUpData(
    var cid: String = "",
    var name: String? = "",
    var articles: List<NavigationData>? = null,
    var children: List<NavigationData>? = null
)

data class NavigationData(
    var title: String? = "",
    var id: String? = "",
    var name: String? = "",
    var link: String?=""
)

