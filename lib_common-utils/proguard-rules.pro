#############################################
# lib_common-utils consumer rules（Moshi 反射适配器）
#############################################

# NullSafeKotlinJsonAdapter 通过 kotlin-reflect 读取主构造参数、可空性与默认值。
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,AnnotationDefault
-keep class kotlin.Metadata { *; }

# 自定义 Moshi 适配器工厂由 Moshi 按类型反射实例化并读取泛型参数。
# 依据：moshi/NullSafeKotlinJsonAdapter.kt、moshi/NullSafeStandardJsonAdapters.java
-keepclassmembers class com.common.utils.moshi.NullSafeKotlinJsonAdapter { *; }
-keepclassmembers class com.common.utils.moshi.NullSafeStandardJsonAdapters { *; }
# DefaultValueProvider 由适配器按接口调用，允许混淆实现，仅保留成员签名。
-keepclassmembers class * implements com.common.utils.moshi.DefaultValueProvider { *; }

# Stroke 由 Moshi 反射适配器按字段名解析（Layout.kt 中的 shape 描边配置）。
-keep class com.common.utils.ext.Stroke { *; }

# JSR-305 注解仅编译期可见。
-dontwarn javax.annotation.**

# 说明：本模块对 Android Framework / MIUI / Flyme 的反射（StatusBarUtil 等）
# 目标均为系统类，不需要保留本模块自身的业务类，故不添加整包 keep。
