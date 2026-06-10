package com.common.security.crypto

import com.common.security.codec.Base64Codec
import com.common.security.codec.HexCodec
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacUtils {
    @JvmStatic
    @JvmOverloads
    fun hmacSha256Hex(
        text: String,
        key: String,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        return HexCodec.encode(hmac(text.toByteArray(charset), key.toByteArray(charset), "HmacSHA256"))
    }

    @JvmStatic
    @JvmOverloads
    fun hmacSha256Base64(
        text: String,
        key: String,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        return Base64Codec.encode(hmac(text.toByteArray(charset), key.toByteArray(charset), "HmacSHA256"))
    }

    @JvmStatic
    fun hmacSha256(bytes: ByteArray, key: ByteArray): ByteArray {
        return hmac(bytes, key, "HmacSHA256")
    }

    @JvmStatic
    fun hmacSha512(bytes: ByteArray, key: ByteArray): ByteArray {
        return hmac(bytes, key, "HmacSHA512")
    }

    @JvmStatic
    fun hmac(bytes: ByteArray, key: ByteArray, algorithm: String): ByteArray {
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(key, algorithm))
        return mac.doFinal(bytes)
    }
}
