package com.common.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object DeviceIdUtil {

    @JvmStatic
    fun getInstance(): DeviceIdUtil = this

    /**
     * 获得设备硬件标识
     *
     * @param context 上下文
     * @return 设备硬件标识
     */
    fun getDeviceId(context: Context): String {
        val sbDeviceId = StringBuilder()

        val androidId = getAndroidId(context)
        val board = Build.BOARD                 // 主板
        val brand = Build.BRAND                 // 系统定制商，品牌
        val device = Build.DEVICE               // 设备参数
        val hardware = Build.HARDWARE           // 硬件名称
        val manufacturer = Build.MANUFACTURER   // 硬件制造商
        val model = Build.MODEL                 // 版本，最终用户可见的名称
        val product = Build.PRODUCT             // 整个产品的名称

        // 追加androidId
        if (!androidId.isNullOrEmpty()) {
            sbDeviceId.append(androidId).append("|")
        }
        // board
        if (!board.isNullOrEmpty()) {
            sbDeviceId.append(board).append("|")
        }
        // 追加brand
        if (!brand.isNullOrEmpty()) {
            sbDeviceId.append(brand).append("|")
        }
        // 追加device
        if (!device.isNullOrEmpty()) {
            sbDeviceId.append(device).append("|")
        }
        // 追加hardware
        if (!hardware.isNullOrEmpty()) {
            sbDeviceId.append(hardware).append("|")
        }
        // 追加manufacturer
        if (!manufacturer.isNullOrEmpty()) {
            sbDeviceId.append(manufacturer).append("|")
        }
        // 追加model
        if (!model.isNullOrEmpty()) {
            sbDeviceId.append(model).append("|")
        }
        // 追加硬件uuid
        if (!product.isNullOrEmpty()) {
            sbDeviceId.append(product)
        }

        LogUtil.e("TAG----->", "info = $sbDeviceId")

        // 生成SHA1，统一DeviceId长度
        if (sbDeviceId.isNotEmpty()) {
            try {
                val hash = getHashByString(sbDeviceId.toString())
                val sha1 = bytesToHex(hash)
                if (sha1.isNotEmpty()) {
                    // 返回最终的DeviceId
                    return sha1
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        // 如果以上硬件标识数据均无法获得，
        // 则DeviceId默认使用系统随机数，这样保证DeviceId不为空
        return UUID.randomUUID().toString().replace("-", "")
    }

    /**
     * 获得设备的AndroidId
     */
    private fun getAndroidId(context: Context): String {
        try {
            return Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return "unknown"
    }

    /**
     * 取SHA1
     */
    private fun getHashByString(data: String): ByteArray {
        return try {
            val messageDigest = MessageDigest.getInstance("SHA1")
            messageDigest.reset()
            messageDigest.update(data.toByteArray(Charsets.UTF_8))
            messageDigest.digest()
        } catch (e: Exception) {
            ByteArray(0)
        }
    }

    /**
     * 转16进制字符串
     */
    private fun bytesToHex(data: ByteArray): String {
        val sb = StringBuilder()
        for (b in data) {
            val stmp = Integer.toHexString(b.toInt() and 0xFF)
            if (stmp.length == 1) {
                sb.append("0")
            }
            sb.append(stmp)
        }
        return sb.toString().uppercase(Locale.CHINA)
    }

    /**
     * 获取手机品牌
     */
    val deviceBrand: String
        get() = Build.BRAND

    /**
     * 获取手机型号
     */
    val deviceModel: String
        get() = Build.MODEL

    /**
     * 获取手机Android 系统SDK
     */
    val deviceSDK: Int
        get() = Build.VERSION.SDK_INT
}