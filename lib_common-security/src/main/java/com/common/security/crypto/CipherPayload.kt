package com.common.security.crypto

import com.common.security.codec.Base64Codec

data class CipherPayload(
    val iv: ByteArray,
    val cipherText: ByteArray
) {
    fun encodeToString(): String {
        return Base64Codec.encode(iv) + SEPARATOR + Base64Codec.encode(cipherText)
    }

    companion object {
        private const val SEPARATOR = ":"

        @JvmStatic
        fun decode(encoded: String): CipherPayload {
            val parts = encoded.split(SEPARATOR)
            require(parts.size == 2) { "Invalid cipher payload." }
            return CipherPayload(
                iv = Base64Codec.decode(parts[0]),
                cipherText = Base64Codec.decode(parts[1])
            )
        }
    }
}
