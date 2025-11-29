package com.dian.demo.di.model

import android.R

/**
 * {
 *     "data": {
 *         "coinInfo": { // 积分和排名可能不是实时的，每天更新
 *             "coinCount": 36662, // 可用
 *             "level": 367, // 可用
 *             "nickname": "",
 *             "rank": "3", // 可用
 *             "userId": 2, // 可用
 *             "username": "x**oyang"
 *         },
 *         "userInfo": {
 *             "admin": false,
 *             "chapterTops": [],
 *             "coinCount": 36662, // 可用
 *             "collectIds": [ // 可用
 *             ],
 *             "email": "623565791@qq.com", // 可用
 *             "icon": "",
 *             "id": 2, // 可用
 *             "nickname": "鸿洋",// 可用
 *             "password": "",
 *             "publicName": "鸿洋",
 *             "token": "",
 *             "type": 0,
 *             "username": "xiaoyang"// 可用
 *         }
 *     },
 *     "errorCode": 0,
 *     "errorMsg": ""
 * }
 */
data class UserInfo(
    val coinInfo: CoinInfo? = null,
    val userInfo: UserInfoBean
)

data class CoinInfo(
    val coinCount: Int? = 0,
    val level: Int? = 0,
    val nickname: String? = "",
    val rank: Int? = 0,
    val userId: String? = "",
    val username: String? = "",

    )

data class UserInfoBean(
val icon: String=""
)