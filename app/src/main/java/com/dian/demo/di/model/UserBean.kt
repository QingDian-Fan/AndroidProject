package com.dian.demo.di.model


data class UserBean(
    var admin:Boolean=false,
    var coinCount:Int,
    var chapterTops:List<Any>,
    var collectIds:List<Int>,
    var email:String,
    var icon:String,
    var id:String,
    var nickname:String,
    var publicName:String,
    var username:String,
    var type:String
)
