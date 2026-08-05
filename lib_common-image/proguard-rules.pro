#############################################
# lib_common-image consumer rules（Glide / Coil）
#############################################

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Glide 在运行期通过固定类名反射查找注解处理器生成的模块实现与请求管理工厂。
# 依据：Glide 官方要求，宿主未使用 AppGlideModule 时这些类可能不存在，故用 -keep 而非 -keepnames。
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep class com.bumptech.glide.GeneratedRequestManagerFactory { *; }

# CommonImage 是本模块唯一对外门面，宿主可能从 Java 侧按名调用（@JvmStatic）。
# 只保留公开静态入口，引擎实现与 Bitmap 工具类允许被裁剪、优化和改名。
# 依据：CommonImage.kt
-keepclassmembers class com.common.image.CommonImage {
    public static <methods>;
}

# ImageEngine 是宿主自定义图片引擎的扩展点（CommonImage.init(customEngine)），
# 接口方法名需保持稳定以便外部实现类二进制兼容。
-keep interface com.common.image.ImageEngine { *; }

# Glide 注解处理器相关的可选引用；本模块未启用 Glide 的 KAPT 处理器。
-dontwarn com.bumptech.glide.annotation.**

# 说明：Coil 2.x 的 AAR 自带 consumer rules（含 coil.util、OkHttp 相关），无需在此重复。
