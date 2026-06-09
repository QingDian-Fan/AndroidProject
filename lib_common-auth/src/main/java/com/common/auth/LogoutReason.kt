package com.common.auth

enum class LogoutReason {
    USER,
    TOKEN_EXPIRED,
    UNAUTHORIZED,
    SESSION_INVALID,
    UNKNOWN
}
