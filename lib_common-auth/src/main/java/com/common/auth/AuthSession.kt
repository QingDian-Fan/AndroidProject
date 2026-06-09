package com.common.auth

data class AuthSession @JvmOverloads constructor(
    val token: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val cookie: String? = null,
    val expireAt: Long = 0L,
    val extras: Map<String, String> = emptyMap()
) {
    fun hasCredential(): Boolean {
        return !token.isNullOrBlank() || !cookie.isNullOrBlank() || !userId.isNullOrBlank()
    }

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return expireAt > 0L && now >= expireAt
    }
}
