package com.common.utils.encryption

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.security.*
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

class RSAUtils {
    private var publicKey: PublicKey?
    private var privateKey: PrivateKey?

    val KEY_ALGORITHM = "RSA"
    private val PUBLIC_KEY = "RSAPublicKey"
    private val PRIVATE_KEY = "RSAPrivateKey"
    val SIGNATURE_ALGORITHM = "MD5withRSA"

    //公钥和私钥Base64字符串
    var publicKeyString = ""
    var privateKeyString = ""
    /**
     * RSA最大加密明文大小
     */
    private val MAX_ENCRYPT_BLOCK = 117

    /**
     * RSA最大解密密文大小
     */
    private val MAX_DECRYPT_BLOCK = 128

    private val keySize = 1024
    private val seedStr = "test"

    init {
        initKey()
        publicKey = getPublicKey(publicKeyString)
        privateKey = getPrivateKey(privateKeyString)
    }

    /**
     * 初始化秘钥
     */
    private fun initKey() {
        //2，通过秘钥对生成器KeyPairGenerator 生成公钥和私钥
        val keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        keyGen.initialize(keySize, SecureRandom(seedStr.toByteArray()))
        //使用公钥进行加密，私钥进行解密（也可以反过来使用）
        val keyPair = keyGen.generateKeyPair()
        publicKey = keyPair.public
        privateKey = keyPair.private
        val privateEncoded = privateKey?.encoded
        val publicEncoded = publicKey?.encoded
        //生成公钥和私钥64编码字符串
        val publicKey64 = Base64.encodeToString(publicEncoded, Base64.NO_WRAP)
        val privateKey64 = Base64.encodeToString(privateEncoded, Base64.NO_WRAP)
       // LogUtil.d("公钥：$publicKey64")
       // LogUtil.d("私钥：$privateKey64")
        publicKeyString = publicKey64
        privateKeyString = privateKey64
    }

    /**
     * 保存秘钥到文件进行存储
     */
    private fun saveKeyToFile() {
        var oosPublic: ObjectOutputStream? = null
        var oosPrivate: ObjectOutputStream? = null
        try {
            oosPublic = ObjectOutputStream(FileOutputStream(PUBLIC_KEY))
            oosPrivate = ObjectOutputStream(FileOutputStream(PRIVATE_KEY))
            oosPublic.writeObject(publicKey)
            oosPrivate.writeObject(privateKey)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            oosPublic?.close()
            oosPrivate?.close()
        }
    }

    /**
     * 根据字符串生成私钥
     * @param dataString:Base64转码后的私钥字符串
     */
    private fun getPrivateKey(dataString: String): PrivateKey {
        val decode = Base64.decode(dataString, Base64.NO_WRAP)
        val pkcs8EncodedKeySpec =
            PKCS8EncodedKeySpec(decode)
        val kf =
            KeyFactory.getInstance(KEY_ALGORITHM)
        return kf.generatePrivate(pkcs8EncodedKeySpec)
    }

    /**
     * 根据字符串生成公钥
     * @param dataString:Base64转码后的公钥字符串
     */
    private fun getPublicKey(dataString: String): PublicKey {
        val decode = Base64.decode(dataString, Base64.NO_WRAP)
        //      PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(decode); //java底层 RSA公钥只支持X509EncodedKeySpec这种格式
        val x509EncodedKeySpec = X509EncodedKeySpec(decode)
        val kf = KeyFactory.getInstance(KEY_ALGORITHM)
        return kf.generatePublic(x509EncodedKeySpec)
    }

    //************************加密解密**************************
    /**
     * 加密
     * 使用私钥加密
     */
    /**
     * 加密
     * @param data
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws IOException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class, NoSuchPaddingException::class, IllegalBlockSizeException::class, BadPaddingException::class, InvalidKeyException::class, IOException::class)
    fun encrypt(data: String): String? {
        val ci =
            Cipher.getInstance(KEY_ALGORITHM)
        ci.init(Cipher.ENCRYPT_MODE, privateKey)
        val bytes = data.toByteArray()
        val inputLen = bytes.size
        var offLen = 0 //偏移量
        var i = 0
        val bops = ByteArrayOutputStream()
        while (inputLen - offLen > 0) {
            var cache: ByteArray?
            cache = if (inputLen - offLen > MAX_ENCRYPT_BLOCK) {
                ci.doFinal(bytes, offLen, MAX_ENCRYPT_BLOCK)
            } else {
                ci.doFinal(bytes, offLen, inputLen - offLen)
            }
            bops.write(cache)
            i++
            offLen = MAX_ENCRYPT_BLOCK * i
        }
        bops.close()
        val encryptedData = bops.toByteArray()
//        return Base64.getEncoder().encodeToString(encryptedData)
        return Base64.encodeToString(encryptedData, Base64.NO_WRAP)

    }


    /**
     * 解密
     * 使用公钥解密
     * @param data
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws NoSuchPaddingException
     * @throws InvalidKeySpecException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws IOException
     */
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class, NoSuchPaddingException::class, InvalidKeySpecException::class, IllegalBlockSizeException::class, BadPaddingException::class, IOException::class)
    fun decrypt(data: String): String {
        val ci = Cipher.getInstance(KEY_ALGORITHM)
        ci.init(Cipher.DECRYPT_MODE, publicKey)
        val bytes = Base64.decode(data, Base64.NO_WRAP)
        val inputLen = bytes.size
        var offLen = 0
        var i = 0
        val byteArrayOutputStream = ByteArrayOutputStream()
        while (inputLen - offLen > 0) {
            val cache: ByteArray? = if (inputLen - offLen > MAX_DECRYPT_BLOCK) {
                ci.doFinal(bytes, offLen, MAX_DECRYPT_BLOCK)
            } else {
                ci.doFinal(bytes, offLen, inputLen - offLen)
            }
            byteArrayOutputStream.write(cache)
            i++
            offLen = MAX_DECRYPT_BLOCK * i
        }
        byteArrayOutputStream.close()
        val byteArray = byteArrayOutputStream.toByteArray()
        return String(byteArray)
    }

    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { RSAUtils() }
    }
}