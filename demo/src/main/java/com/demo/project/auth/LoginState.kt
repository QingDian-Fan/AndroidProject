package com.demo.project.auth

import com.common.auth.AuthManager
import com.dian.annotation.CheckLogin

object LoginState {
    @JvmStatic
    @CheckLogin
    fun isLogin(): Boolean = AuthManager.isLogin()
}
