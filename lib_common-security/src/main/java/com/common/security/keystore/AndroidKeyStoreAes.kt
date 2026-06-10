package com.common.security.keystore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.common.security.crypto.AesGcmCrypto
import com.common.security.crypto.CipherPayload
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object AndroidKeyStoreAes {
    @JvmStatic
    @JvmOverloads
    fun getOrCreateSecretKey(alias: String, userAuthenticationRequired: Boolean = false): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(userAuthenticationRequired)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    @JvmStatic
    fun deleteKey(alias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    @JvmStatic
    @JvmOverloads
    fun encrypt(alias: String, data: ByteArray, aad: ByteArray? = null): CipherPayload {
        return AesGcmCrypto.encrypt(data, getOrCreateSecretKey(alias), aad)
    }

    @JvmStatic
    @JvmOverloads
    fun decrypt(alias: String, payload: CipherPayload, aad: ByteArray? = null): ByteArray {
        return AesGcmCrypto.decrypt(payload, getOrCreateSecretKey(alias), aad)
    }

    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
}
