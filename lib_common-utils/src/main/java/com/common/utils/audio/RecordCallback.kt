package com.common.utils.audio

fun interface RecordCallback {
    /**
     * 数据回调
     *
     * @param bytes 数据
     * @param len   数据有效长度，-1 时表示数据结束
     */
    fun onRecord(bytes: ByteArray, len: Int)
}