package com.dian.demo.di.model

/**
 * {
 * "coinCount": 34,
 * "date": 1764639814000,
 * "desc": "2025-12-02 09:43:34 签到 , 积分：27 + 7",
 * "id": 924140,
 * "reason": "签到",
 * "type": 1,
 * "userId": 22385,
 * "userName": "QingDian_Fan"
 * }
 */
data class CoinCount(
    var coinCount: String,
    var date: String,
    var desc: String,
    var id: String,
    var reason: String,
    var type: String,
    var userId: String,
    var username: String,
    var anim: Boolean = false
)
