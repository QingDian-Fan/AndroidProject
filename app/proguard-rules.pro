# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\android-sdk-windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
# copyright jeff

# 基本指令区
-optimizationpasses 5
-dontskipnonpubliclibraryclassmembers
-printmapping proguardMapping.txt
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-keepattributes *Annotation*,InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# 默认保留区
# 继承activity,application,service,broadcastReceiver,contentprovider....不进行混淆
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.support.multidex.MultiDexApplication
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService
-keep class android.support.** {*;}

-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
# 这个主要是在layout 中写的onclick方法android:onclick="onClick"，不进行混淆
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keep class **.R$* { *;}

-keepclassmembers class * {
    void *(*Event);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# natvie 方法不混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保持 Parcelable 不被混淆
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
# webview
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
    public boolean *(android.webkit.WebView, java.lang.String);
}
-keepclassmembers class * extends android.webkit.WebViewClient {
    public void *(android.webkit.WebView, java.lang.String);
}

#zxing
-keep class com.google.zxing.** {*;}
-dontwarn com.google.zxing.**
#okhttp3.x
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *;}
-dontwarn okio.**
#retrofit
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okio.**

# gson
# Gson 基本配置
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Gson 注解字段
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Gson TypeAdapter / 反射支持
-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.InstanceCreator { *; }

# TypeToken 支持
-keep class com.google.gson.reflect.TypeToken { *; }

# 模型类（推荐）
-keep class com.exturing.uiagent.model.** { *; }


#glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
# ViewBinding & DataBinding
-keepclassmembers class * implements android.viewbinding.ViewBinding {
  public static * inflate(android.view.LayoutInflater);
  public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
  public static * bind(android.view.View);
}

-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

# 保留自定义控件(继承自View)不能被混淆
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
    *** get* ();
}

# 保留AspectJ的注解：
-keepattributes Annotation

# 防止AspectJ生成的代码被优化：
-dontoptimize

# 防止AspectJ生成的代码被混淆：
-keep class * implements org.aspectj.lang.annotation.* {*;}

# 保留AspectJ的切点表达式：
-keepclassmembers class * {
    @org.aspectj.lang.annotation.Pointcut *;
}
-keepclassmembers class * {
    @org.aspectj.lang.annotation.Around *;
}
-keepclassmembers enum * {
public static **[] values();
public static ** valueOf(java.lang.String);
}


-keeppackagenames com.dian.demo

#实体类
-keep class com.dian.demo.di.model.** { *; }
-keep class com.dian.demo.di.vm.** { *; }
-keep class com.dian.demo.ui.activity.** { *; }
-keep class com.dian.demo.ui.img.ImageSelectActivity.** { *; }
#aop
-keep class com.dian.demo.utils.aop.** { *; }

-keepclassmembers class * {
    @com.dian.demo.utils.aop.SingleClick *;
}

-keepclassmembers class * {
    @com.dian.demo.utils.aop.CheckPermissions *;
}

-keepclassmembers class * {
    @com.dian.demo.utils.aop.SingleClick *;
}





