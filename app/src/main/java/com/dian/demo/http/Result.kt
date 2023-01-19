package com.dian.demo.http

/**
 * @author: QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2020/8/14 7:06 PM
 * @description: 成功的网络请求返回的数据模型
 * @since: 1.0.0
 */
data class Result<T>(
    val errorCode: Int,
    val errorMsg: String,
    val data: T?=null,
) {
    /**
     * 判断本次请求返回的响应是否为成功响应，此方法一定要实现
     */
    fun isSuccessful(): Boolean {
        return errorCode == 0
    }

    fun isNotLogin(): Boolean {
        return errorCode == -1001
    }
}

