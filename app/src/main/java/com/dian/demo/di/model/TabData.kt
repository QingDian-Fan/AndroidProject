package com.dian.demo.di.model

import androidx.annotation.DrawableRes

data class TabData(
    val id:Int,
    @DrawableRes
    val img:Int,
    val title:String
)
