package com.common.security.crypto

import com.common.security.codec.Base64Codec
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.interfaces.RSAKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

object RsaCrypto {
    @JvmStatic
    @JvmOverloads
    fun generateKeyPair(keySize: Int = DEFAULT_KEY_SIZE): KeyPair {
        return KeyPairGenerator.getInstance(KEY_ALGORITHM).apply {
            initialize(keySize)
        }.generateKeyPair()
    }

    @JvmStatic
    fun publicKeyToBase64(publicKey: PublicKey): String = Base64Codec.encode(publicKey.encoded)

    @JvmStatic
    fun privateKeyToBase64(privateKey: PrivateKey): String = Base64Codec.encode(privateKey.encoded)

    @JvmStatic
    fun publicKeyFromBase64(publicKey: String): PublicKey {
        val spec = X509EncodedKeySpec(Base64Codec.decode(publicKey))
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(spec)
    }

    @JvmStatic
    fun privateKeyFromBase64(privateKey: String): PrivateKey {
        val spec = PKCS8EncodedKeySpec(Base64Codec.decode(privateKey))
        return KeyFactory.getInstance(KEY_ALGORITHM).generatePrivate(spec)
    }

    @JvmStatic
    @JvmOverloads
    fun encryptByPublicKey(
        data: ByteArray,
        publicKey: PublicKey,
        transformation: String = DEFAULT_TRANSFORMATION
    ): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinalByBlock(data, publicKey.blockSizeBytes(transformation, true))
    }

    @JvmStatic
    @JvmOverloads
    fun decryptByPrivateKey(
        encryptedData: ByteArray,
        privateKey: PrivateKey,
        transformation: String = DEFAULT_TRANSFORMATION
    ): ByteArray {
        val cipher = Cipher.getInstance(transformation)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        return cipher.doFinalByBlock(encryptedData, privateKey.blockSizeBytes(transformation, false))
    }

    @JvmStatic
    @JvmOverloads
    fun encryptToBase64(
        text: String,
        publicKeyBase64: String,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        return Base64Codec.encode(encryptByPublicKey(text.toByteArray(charset), publicKeyFromBase64(publicKeyBase64)))
    }

    @JvmStatic
    @JvmOverloads
    fun decryptFromBase64(
        encryptedBase64: String,
        privateKeyBase64: String,
        charset: Charset = StandardCharsets.UTF_8
    ): String {
        val bytes = decryptByPrivateKey(Base64Codec.decode(encryptedBase64), privateKeyFromBase64(privateKeyBase64))
        return String(bytes, charset)
    }

    @JvmStatic
    @JvmOverloads
    fun signSha256(data: ByteArray, privateKey: PrivateKey, algorithm: String = DEFAULT_SIGN_ALGORITHM): ByteArray {
        val signature = Signature.getInstance(algorithm)
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    @JvmStatic
    @JvmOverloads
    fun verifySha256(
        data: ByteArray,
        signatureBytes: ByteArray,
        publicKey: PublicKey,
        algorithm: String = DEFAULT_SIGN_ALGORITHM
    ): Boolean {
        val signature = Signature.getInstance(algorithm)
        signature.initVerify(publicKey)
        signature.update(data)
        return signature.verify(signatureBytes)
    }

    @JvmStatic
    fun signSha256Base64(text: String, privateKeyBase64: String): String {
        return Base64Codec.encode(signSha256(text.toByteArray(), privateKeyFromBase64(privateKeyBase64)))
    }

    @JvmStatic
    fun verifySha256Base64(text: String, signatureBase64: String, publicKeyBase64: String): Boolean {
        return verifySha256(
            data = text.toByteArray(),
            signatureBytes = Base64Codec.decode(signatureBase64),
            publicKey = publicKeyFromBase64(publicKeyBase64)
        )
    }

    private fun Cipher.doFinalByBlock(data: ByteArray, blockSize: Int): ByteArray {
        require(blockSize > 0) { "Invalid RSA block size." }
        val output = ByteArrayOutputStream()
        var offset = 0
        while (offset < data.size) {
            val count = minOf(blockSize, data.size - offset)
            output.write(doFinal(data, offset, count))
            offset += count
        }
        return output.toByteArray()
    }

    private fun java.security.Key.blockSizeBytes(transformation: String, encrypt: Boolean): Int {
        val keyBytes = ((this as RSAKey).modulus.bitLength() + 7) / 8
        if (!encrypt) return keyBytes

        return when {
            transformation.contains("OAEPWithSHA-256", ignoreCase = true) -> keyBytes - 2 * 32 - 2
            transformation.contains("OAEP", ignoreCase = true) -> keyBytes - 2 * 20 - 2
            transformation.contains("PKCS1Padding", ignoreCase = true) -> keyBytes - 11
            else -> keyBytes
        }
    }

    private const val KEY_ALGORITHM = "RSA"
    private const val DEFAULT_KEY_SIZE = 2048
    private const val DEFAULT_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
    private const val DEFAULT_SIGN_ALGORITHM = "SHA256withRSA"
}
