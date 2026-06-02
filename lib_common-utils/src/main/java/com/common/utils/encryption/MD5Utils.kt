package com.common.utils.encryption

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object MD5Utils {

    fun get(text: String): String {
        var result = ""
        try {
            val md = MessageDigest.getInstance("MD5");
            val digest = md.digest(text.toByteArray())
            result = toHexString(digest);
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace();
        }
        return result
    }

    private fun toHexString(digest: ByteArray): String {
        val sb = StringBuilder()
        var hexStr: String? = null;
        digest.forEach {
            hexStr = Integer.toHexString(it.toInt() and 0xFF)//& 0xFF处理负数
            if (hexStr?.length == 1) {//长度等于1，前面进行补0，保证最后的字符串长度为32
                hexStr = "0$hexStr";
            }
            sb.append(hexStr);
        }
        return sb.toString();
    }



}