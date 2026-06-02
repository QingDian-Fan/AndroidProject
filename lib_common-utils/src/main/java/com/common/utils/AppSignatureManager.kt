package com.common.utils

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import java.security.MessageDigest

class AppSignatureManager private constructor() {

    private val TAG = "TAG----->signature"

    private val signMap = HashMap<Type, ArrayList<String>>()

    private enum class Type(val value: String) {
        MD5("MD5"), SHA1("SHA1"), SHA256("SHA256")
    }

    private object InnerClass {
        val INSTANCE = AppSignatureManager()
    }

    companion object {
        @JvmStatic
        fun getInstance(): AppSignatureManager = InnerClass.INSTANCE
    }

    fun getMD5(context: Context): String {
        val list = getSignInfo(context, Type.MD5)
        return if (!list.isNullOrEmpty()) list[0] else ""
    }

    fun getSHA1(context: Context): String {
        val list = getSignInfo(context, Type.SHA1)
        return if (!list.isNullOrEmpty()) list[0] else ""
    }

    fun getSHA256(context: Context): String {
        val list = getSignInfo(context, Type.SHA256)
        return if (!list.isNullOrEmpty()) list[0] else ""
    }

    private fun getSignInfo(context: Context?, type: Type?): ArrayList<String>? {
        if (context == null || type == null) {
            return null
        }
        val packageName = context.packageName ?: return null
        signMap[type]?.let { return it }

        val list = ArrayList<String>()
        try {
            val signs = getSignatures(context, packageName)
            if (signs != null) {
                for (sig in signs) {
                    val tmp = when (type) {
                        Type.MD5 -> getSignatureByteString(sig, Type.MD5)
                        Type.SHA1 -> getSignatureByteString(sig, Type.SHA1)
                        Type.SHA256 -> getSignatureByteString(sig, Type.SHA256)
                    }
                    list.add(tmp)
                }
            }
        } catch (e: Exception) {
            LogUtil.e(TAG, e.message ?: "")
        }
        signMap[type] = list
        return list
    }

    private fun getSignatures(context: Context, packageName: String): Array<Signature>? {
        try {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager
                .getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            return packageInfo.signatures
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getSignatureByteString(sig: Signature, type: Type): String {
        val hexBytes = sig.toByteArray()
        var fingerprint = "error!"
        try {
            val digest = MessageDigest.getInstance(type.value)
            val digestBytes = digest.digest(hexBytes)
            val sb = StringBuilder()
            for (digestByte in digestBytes) {
                sb.append(
                    Integer.toHexString((digestByte.toInt() and 0xFF) or 0x100)
                        .substring(1, 3)
                        .uppercase()
                )
                sb.append(":")
            }
            fingerprint = sb.substring(0, sb.length - 1)
        } catch (e: Exception) {
            LogUtil.e(TAG, "getSignatureByteString failed", e.message)
        }
        return fingerprint
    }
}