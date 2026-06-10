# lib_common-security

通用安全基础能力模块。模块不引入第三方安全库，默认使用 Android/JDK 标准安全 API。

## 能力边界

本模块负责：

- 编码：Base64、Hex、指纹格式化
- 摘要：MD5、SHA-1、SHA-256、SHA-512、文件摘要
- HMAC：HmacSHA256、HmacSHA512
- 对称加密：AES-GCM
- 非对称加密：RSA-OAEP、SHA256withRSA 签名/验签
- Android Keystore：AES 密钥创建、加密、解密、删除
- App 签名：MD5/SHA1/SHA256 指纹读取和校验
- 环境检测：debuggable、ADB、Root、模拟器

本模块不负责：

- 登录鉴权：留给 `lib_common-auth`
- 网络证书固定：建议放在 `lib_common-http` 的 OkHttp 配置层
- 风控策略：这里只提供检测结果，具体拦截由业务决定

## 推荐用法

```kotlin
val encrypted = AesGcmCrypto.encryptToString("hello", "password")
val raw = AesGcmCrypto.decryptToString(encrypted, "password")
```

```kotlin
val signOk = AppSignatureUtils.verifySha256(context, "AA:BB:CC:...")
```

```kotlin
val result = SecurityEnvironment.collect(context)
if (result.risky) {
    // handle risk
}
```

```kotlin
val payload = AndroidKeyStoreAes.encrypt("account_token", tokenBytes)
val tokenBytes = AndroidKeyStoreAes.decrypt("account_token", payload)
```

## 注意

- 新代码默认使用 AES-GCM，不再使用 AES/ECB。
- RSA 默认使用 `RSA/ECB/OAEPWithSHA-256AndMGF1Padding`。
- MD5/SHA1 只建议用于兼容旧协议或展示指纹，新接口校验优先使用 SHA-256。
