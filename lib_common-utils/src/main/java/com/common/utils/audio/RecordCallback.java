package com.common.utils.audio;

public interface RecordCallback {
    /**
     * 数据回调
     *
     * @param bytes 数据
     * @param len   数据有效长度，-1时表示数据结束
     */
    void onRecord(byte[] bytes, int len);
}
