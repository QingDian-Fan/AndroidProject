package com.common.security

import com.common.security.crypto.SecureRandomUtils

object CommonSecurity {
    @JvmStatic
    fun randomBytes(size: Int): ByteArray = SecureRandomUtils.randomBytes(size)

    @JvmStatic
    fun randomHex(size: Int): String = SecureRandomUtils.randomHex(size)

    @JvmStatic
    fun randomBase64(size: Int): String = SecureRandomUtils.randomBase64(size)
}
