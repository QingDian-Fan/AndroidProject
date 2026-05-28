package com.dian.demo.utils

import com.dian.annotation.CheckLogin
import com.dian.demo.di.model.UserInfo
import com.project.common.utils.datastore.AppDataStore
import com.project.common.utils.LogUtil
import com.project.common.utils.MoshiUtil
import kotlin.text.ifEmpty

object LoginUtil {
    @CheckLogin
    @JvmStatic
    fun isLogin(): Boolean {
        val cookies = AppDataStore.getData("www.wanandroid.com", "")
        val cookie: String = cookies.ifEmpty { "" }
        val isLogin = cookie.isNotEmpty()
        LogUtil.e("TAG--->","isLogin::${isLogin}")
        return isLogin
    }

    fun saveLoginInfo(data: UserInfo) {
        AppDataStore.putData("login_user_info", MoshiUtil.toJson(data))
    }

    fun getLoginInfo(): UserInfo? {
        if (isLogin()) {
            val dataString = AppDataStore.getData("login_user_info", "")
            if (dataString.isNotEmpty()) {
                val data = MoshiUtil.fromJson<UserInfo>(dataString)
                return data
            }
        }
        return null
    }

    fun clearLoginInfo() {
        AppDataStore.clearKey("login_user_info")
        AppDataStore.clearKey("www.wanandroid.com")
    }
}