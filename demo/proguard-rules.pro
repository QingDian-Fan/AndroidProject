#############################################
# App rules
#############################################

# Keep useful stack traces for release crash analysis.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures, annotations and enclosing method metadata used by
# Retrofit, Room, Moshi, Kotlin reflection and AspectJ.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,AnnotationDefault
-keep class kotlin.Metadata { *; }

#############################################
# Android entry points / project reflection
#############################################

# Activity/Fragment/Application/Service/BroadcastReceiver names are usually
# handled by AGP, but these packages are also opened by string reflection
# (SchemeUtils / LoginHookUtil), so keep their binary names stable.
-keepnames class com.demo.project.ProjectApplication
-keepnames class com.demo.project.ui.activity.** { *; }
-keepnames class com.demo.project.ui.fragment.** { *; }
-keepclassmembers class com.demo.project.ui.activity.** {
    public static void start(android.content.Context, ...);
}

# APT login helper is found through Class.forName/getDeclaredMethod.
-keep class com.dian.demo.apt.** { *; }
-keep @com.dian.annotation.LoginPage class * { *; }
-keep @com.dian.annotation.RequireLogin class * { *; }
-keepclassmembers class * {
    @com.dian.annotation.CheckLogin <methods>;
}

#############################################
# ViewBinding / View reflection
#############################################

# BaseAppBindActivity/BaseAppBindFragment find generated binding classes and
# invoke static bind/inflate methods through reflection.
-keep class **.databinding.*Binding { *; }
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * bind(android.view.View);
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
-keep class * extends com.common.ui.BaseAppBindActivity { *; }
-keep class * extends com.common.ui.BaseAppVMActivity { *; }
-keep class * extends com.common.ui.BaseAppBindFragment { *; }
-keep class * extends com.common.ui.BaseAppVMFragment { *; }
-keep class * extends com.common.ui.skin.BaseSkinBindActivity { *; }
-keep class * extends com.common.ui.skin.BaseSkinVMActivity { *; }
-keep class * extends com.common.ui.skin.BaseSkinBindFragment { *; }
-keep class * extends com.common.ui.skin.BaseSkinVMFragment { *; }
-keep class com.common.ui.BaseAppBindActivity { *; }
-keep class com.common.ui.BaseAppVMActivity { *; }
-keep class com.common.ui.BaseAppBindFragment { *; }
-keep class com.common.ui.BaseAppVMFragment { *; }
-keep class com.common.ui.skin.BaseSkinBindActivity { *; }
-keep class com.common.ui.skin.BaseSkinVMActivity { *; }
-keep class com.common.ui.skin.BaseSkinBindFragment { *; }
-keep class com.common.ui.skin.BaseSkinVMFragment { *; }

# XML inflated custom views must keep their constructors.
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

#############################################
# WebView JS bridge
#############################################

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.common.weight.webview.webview.BaseWebView {
    @android.webkit.JavascriptInterface <methods>;
}

#############################################
# JSON / HTTP / Room
#############################################

# This project uses Moshi reflective adapters and Gson for these runtime
# protocol models. Keep field/constructor names stable.
-keep class com.common.http.Result { *; }
-keep class com.common.http.HttpError { *; }
-keep class com.common.http.ResponseHolder { *; }
-keep class com.common.http.ResponseHolder$* { *; }
-keep class com.common.http.HttpClientConfig { *; }
-keep class com.common.http.room.** { *; }
-keep class com.common.weight.webview.bean.** { *; }
-keep class com.common.utils.ext.Stroke { *; }

# Retrofit API annotations and suspend/generic signatures.
-keep interface com.common.http.RequestService { *; }
-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

# Room generated implementation and annotated models/DAO.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverters class * { *; }
-dontwarn androidx.room.paging.**

#############################################
# AspectJ / AOP
#############################################

-keep @org.aspectj.lang.annotation.Aspect class * { *; }
-keep class com.common.aop.** { *; }
-keep class **AjcClosure* { *; }
-keepclassmembers class * {
    *** ajc$*(...);
}
-dontwarn org.aspectj.**

#############################################
# Native / OpenCV / scanner
#############################################

-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class org.opencv.** { *; }
-keep class com.common.scan.** { *; }
-dontwarn org.opencv.**

# FFmpeg 播放内核（lib_common-player）的 native 方法与 onNativeXxx JNI 回调由该模块的
# consumer-rules.pro 保留，这里无需重复，仅作说明。

#############################################
# 音视频播放（player engine）
#############################################

# AudioPlayerActivity 通过 intent 传 AudioEngineType.name，再用 valueOf(String) 解析，
# 需保留枚举名称并阻止 R8 的 enum 优化（unboxing）。
-keepclassmembers enum com.demo.project.player.audio.AudioEngineType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Media3/ExoPlayer 随 AAR 自带 consumer proguard 规则，通常无需手写；保险起见忽略告警。
-dontwarn androidx.media3.**

#############################################
# Share SDKs / media / misc third party
#############################################

-keep class com.tencent.** { *; }
-keep class com.sina.weibo.sdk.** { *; }
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl { *; }
-keep public class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-dontwarn com.tencent.**
-dontwarn com.sina.weibo.sdk.**
-dontwarn com.bumptech.glide.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
