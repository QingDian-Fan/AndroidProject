package com.common.security.codec

import android.util.Base64
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object Base64Codec {
    @JvmStatic
    @JvmOverloads
    fun encode(bytes: ByteArray, flags: Int = Base64.NO_WRAP): String {
        return Base64.encodeToString(bytes, flags)
    }

    @JvmStatic
    @JvmOverloads
    fun encodeString(text: String, charset: Charset = StandardCharsets.UTF_8): String {
        return encode(text.toByteArray(charset))
    }

    @JvmStatic
    @JvmOverloads
    fun decode(text: String, flags: Int = Base64.NO_WRAP): ByteArray {
        return Base64.decode(text, flags)
    }

    @JvmStatic
    @JvmOverloads
    fun decodeToString(text: String, charset: Charset = StandardCharsets.UTF_8): String {
        return String(decode(text), charset)
    }

    @JvmStatic
    fun encodeUrlSafe(bytes: ByteArray): String {
        return encode(bytes, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    @JvmStatic
    fun decodeUrlSafe(text: String): ByteArray {
        return decode(text, Base64.NO_WRAP or Base64.URL_SAFE)
    }
}
