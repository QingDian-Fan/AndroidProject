package com.common.auth

interface AuthTokenProvider {
    fun getAccessToken(): String?

    fun getTokenHeaderName(): String

    fun getAuthorizationHeaderValue(): String?
}
