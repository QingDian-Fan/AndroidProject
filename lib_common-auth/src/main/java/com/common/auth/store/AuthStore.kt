package com.common.auth.store

import com.common.auth.AuthSession

interface AuthStore {
    fun saveSession(session: AuthSession)

    fun getSession(): AuthSession?

    fun clearSession()
}
