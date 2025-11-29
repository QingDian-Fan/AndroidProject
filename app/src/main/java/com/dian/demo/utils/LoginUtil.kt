package com.dian.demo.utils

import com.dian.annotation.CheckLogin
import com.dian.demo.utils.datastore.AppDataStore
import kotlin.text.ifEmpty

object LoginUtil {
    @CheckLogin
    @JvmStatic
    fun isLogin(): Boolean {
        val cookies = AppDataStore.getData("www.wanandroid.com", "")
        val cookie: String = cookies.ifEmpty { "" }
        return cookie.isNotEmpty()
    }
}