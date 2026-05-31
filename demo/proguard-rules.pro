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
