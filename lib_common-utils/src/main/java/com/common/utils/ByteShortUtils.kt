package com.common.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

object ByteShortUtils {

    @JvmStatic
    fun getInstance(): ByteShortUtils = this

    fun bytesToShort(bytes: ByteArray?): ShortArray? {
        if (bytes == null) {
            return null
        }
        val shorts = ShortArray(bytes.size / 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts)
        return shorts
    }

    fun shortToBytes(shorts: ShortArray?): ByteArray? {
        if (shorts == null) {
            return null
        }
        val bytes = ByteArray(shorts.size * 2)
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts)
        return bytes
    }
}