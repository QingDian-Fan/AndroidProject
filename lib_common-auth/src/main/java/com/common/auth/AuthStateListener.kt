package com.common.auth

interface AuthStateListener {
    fun onLogin(session: AuthSession) = Unit

    fun onLogout(reason: LogoutReason) = Unit
}
