-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Third-party share SDK callbacks and entry classes are discovered by package/name.
-keep class com.common.share.** { *; }
-keep class com.tencent.** { *; }
-keep class com.sina.weibo.sdk.** { *; }

# Glide module discovery.
-keep public class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

-dontwarn com.tencent.**
-dontwarn com.sina.weibo.sdk.**
-dontwarn com.bumptech.glide.**
