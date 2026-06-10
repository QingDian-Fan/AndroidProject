package com.common.security.crypto

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesGcmCrypto {
    @JvmStatic
    @JvmOverloads
    fun encrypt(
        data: ByteArray,
        key: ByteArray,
        aad: ByteArray? = null
    ): CipherPayload {
        return encrypt(data, key.toAesSecretKey(), aad)
    }

    @JvmStatic
    @JvmOverloads
    fun encrypt(
        data: ByteArray,
        secretKey: SecretKey,
        aad: ByteArray? = null
    ): CipherPayload {
        val iv = SecureRandomUtils.randomBytes(IV_SIZE)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, iv))
        aad?.let { cipher.updateAAD(it) }
        return CipherPayload(iv = iv, cipherText = cipher.doFinal(data))
    }

    @JvmStatic
    @JvmOverloads
    fun decrypt(
        payload: CipherPayload,
        key: ByteArray,
        aad: ByteArray? = null
    ): ByteArray {
        return decrypt(payload, key.toAesSecretKey(), aad)
    }

    @JvmStatic
    @JvmOverloads
    fun decrypt(
        payload: CipherPayload,
        secretKey: SecretKey,
        aad: ByteArray? = null
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE_BITS, payload.iv))
        aad?.let { cipher.updateAAD(it) }
        return cipher.doFinal(payload.cipherText)
    }

    @JvmStatic
    @JvmOverloads
    fun encryptToString(
        text: String,
        key: String,
        aad: String? = null,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        val payload = encrypt(
            data = text.toByteArray(charset),
            key = DigestUtils.sha256Bytes(key),
            aad = aad?.toByteArray(charset)
        )
        return payload.encodeToString()
    }

    @JvmStatic
    @JvmOverloads
    fun decryptToString(
        encodedPayload: String,
        key: String,
        aad: String? = null,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        val data = decrypt(
            payload = CipherPayload.decode(encodedPayload),
            key = DigestUtils.sha256Bytes(key),
            aad = aad?.toByteArray(charset)
        )
        return String(data, charset)
    }

    @JvmStatic
    fun ByteArray.toAesSecretKey(): SecretKey {
        require(size == 16 || size == 24 || size == 32) {
            "AES key must be 16, 24, or 32 bytes."
        }
        return SecretKeySpec(this, KEY_ALGORITHM)
    }

    private const val KEY_ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE_BITS = 128
}
