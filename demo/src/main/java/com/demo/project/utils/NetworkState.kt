package com.demo.project.utils

/**
 * 网络状态的实体bean
 */
data class NetworkState(
    var responseCode: String? = null,
    var success: Boolean = true
)