package com.common.security.env

data class SecurityCheckResult(
    val debuggable: Boolean,
    val adbEnabled: Boolean,
    val rooted: Boolean,
    val emulator: Boolean
) {
    val risky: Boolean
        get() = debuggable || adbEnabled || rooted || emulator
}
