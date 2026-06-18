package com.common.security.crypto

import com.common.security.codec.HexCodec
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object DigestUtils {
    @JvmStatic
    fun md5(text: String): String = digestHex(text.toByteArray(), "MD5")

    @JvmStatic
    fun sha1(text: String): String = digestHex(text.toByteArray(), "SHA-1")

    @JvmStatic
    fun sha256(text: String): String = digestHex(text.toByteArray(), "SHA-256")

    @JvmStatic
    fun sha512(text: String): String = digestHex(text.toByteArray(), "SHA-512")

    @JvmStatic
    fun md5(bytes: ByteArray): String = digestHex(bytes, "MD5")

    @JvmStatic
    fun sha1(bytes: ByteArray): String = digestHex(bytes, "SHA-1")

    @JvmStatic
    fun sha256(bytes: ByteArray): String = digestHex(bytes, "SHA-256")

    @JvmStatic
    fun sha512(bytes: ByteArray): String = digestHex(bytes, "SHA-512")

    @JvmStatic
    fun sha256Bytes(text: String): ByteArray = digest(text.toByteArray(), "SHA-256")

    @JvmStatic
    fun digestHex(bytes: ByteArray, algorithm: String): String {
        return HexCodec.encode(digest(bytes, algorithm))
    }

    @JvmStatic
    fun digest(bytes: ByteArray, algorithm: String): ByteArray {
        return MessageDigest.getInstance(algorithm).digest(bytes)
    }

    @JvmStatic
    @JvmOverloads
    fun fileDigestHex(file: File, algorithm: String = "SHA-256"): String {
        val digest = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        FileInputStream(file).use { input ->
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        return HexCodec.encode(digest.digest())
    }

    private const val DEFAULT_BUFFER_SIZE = 8 * 1024
}
