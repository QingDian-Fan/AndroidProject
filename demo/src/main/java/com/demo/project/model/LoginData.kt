package com.demo.project.model

/**
 * {
 *     "data": {
 *         "admin": false,
 *         "chapterTops": [],
 *         "coinCount": 17885,
 *         "collectIds": [
 *             8703,
 *             12080,
 *             15928,
 *             15900,
 *             17016,
 *             3420,
 *             17146,
 *             17563,
 *             19838,
 *             7656,
 *             20635,
 *             11911,
 *             31045,
 *             31043,
 *             31041,
 *             31039
 *         ],
 *         "email": "dian.work@foxmail.com",
 *         "icon": "",
 *         "id": 22385,
 *         "nickname": "Delusion",
 *         "password": "",
 *         "publicName": "Delusion",
 *         "token": "",
 *         "type": 0,
 *         "username": "QingDian_Fan"
 *     },
 *     "errorCode": 0,
 *     "errorMsg": ""
 * }
 */
data class LoginData(
    val id: String?="",
    val username: String?="",
    val email: String?="",
    val token: String?="",
    )
