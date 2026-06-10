package com.common.security.crypto

import com.common.security.codec.Base64Codec
import com.common.security.codec.HexCodec
import java.security.SecureRandom
import java.util.UUID

object SecureRandomUtils {
    private val secureRandom: SecureRandom by lazy { SecureRandom() }

    @JvmStatic
    fun randomBytes(size: Int): ByteArray {
        require(size > 0) { "Random size must be greater than 0." }
        return ByteArray(size).also { secureRandom.nextBytes(it) }
    }

    @JvmStatic
    fun randomHex(size: Int): String = HexCodec.encode(randomBytes(size))

    @JvmStatic
    fun randomBase64(size: Int): String = Base64Codec.encode(randomBytes(size))

    @JvmStatic
    fun uuid(): String = UUID.randomUUID().toString()
}
