#############################################
# lib_common-security consumer rules
#############################################
# 本模块无反射、无 JNI、无生成代码，全部为纯 Kotlin 实现。
# 依据：CommonSecurity.kt 是唯一对外门面（@JvmStatic 公开方法），
# crypto / codec / keystore / env 下均为内部实现，应允许 R8 裁剪、优化与改名。

# 对外门面的公开静态方法名对宿主构成二进制契约，保留方法名，类名允许混淆。
-keepclassmembers class com.common.security.CommonSecurity {
    public static <methods>;
}

# CipherPayload 作为加解密结果在宿主侧按属性读取，保留其成员。
# 依据：crypto/CipherPayload.kt
-keepclassmembers class com.common.security.crypto.CipherPayload { *; }

# SecurityCheckResult 为环境检测结果对象，宿主按属性读取。
# 依据：env/SecurityCheckResult.kt
-keepclassmembers class com.common.security.env.SecurityCheckResult { *; }
