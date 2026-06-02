package com.common.weight.address

import org.json.JSONObject

/**
 * 地址数据项
 *
 * @param name  显示名称（省/市/区）
 * @param value 该节点对应的原始 Json（省、市节点用于继续向下解析；区节点为 null）
 */
data class CityData(
    val name: String,
    val value: JSONObject?
)