############################################################
# proguard-rules (for project)
# Auto-updated for dependencies found in app/build.gradle
# Keep conservative rules for Room / Moshi / Gson / protobuf-lite / walle / skin / cameraX / AspectJ etc.
############################################################

# ---------- 基础配置 ----------
-dontskipnonpubliclibraryclassmembers
-printmapping proguardMapping.txt
-optimizationpasses 5
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-keepattributes *Annotation*,InnerClasses,Signature,SourceFile,LineNumberTable,Exceptions,RuntimeVisibleAnnotations

# 保留 Kotlin 元数据（有助于某些反射/序列化库）
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.jvm.internal.* { *; }
-dontwarn kotlin.**


# 保留 AndroidX / Support 的 @Keep 注解被标记的类和成员
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers @androidx.annotation.Keep class * { *; }
-keep @android.support.annotation.Keep class * { *; }
-keepclassmembers @android.support.annotation.Keep class * { *; }

# ---------- Android 组件（Activity/Service/Receiver/Provider/Application） ----------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.support.multidex.MultiDexApplication
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep class android.support.** { *; }
-keep class **.R$* { *; }

# layout 中自定义 View 保留构造器及常用 getter/setter
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    public * get*();
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# xml onClick 方法
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# ---------- Serializable / Parcelable ----------
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

# ---------- Native 方法 ----------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---------- Gson（你也用了 Retrofit 的 converter-gson） ----------
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class * extends com.google.gson.TypeAdapter { *; }
-keep class * implements com.google.gson.JsonSerializer { *; }
-keep class * implements com.google.gson.JsonDeserializer { *; }
-keep class * implements com.google.gson.InstanceCreator { *; }
-keep class com.google.gson.reflect.TypeToken { *; }

# ---------- Moshi ----------
# 保留注解（@JsonClass）和使用反射/Codegen 的类
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.JsonName <fields>;
}
# 如果使用 JsonClass(generateAdapter = true) 并依赖 codegen，这里也保留可能生成的适配器名（保守策略）
-keep class *JsonAdapter { *; }
-keep class *JsonAdapter$* { *; }

# ---------- Retrofit / OkHttp / Okio ----------
-dontwarn retrofit.**
-keep class retrofit.** { *; }
-dontwarn com.squareup.okhttp3.**
-keep class com.squareup.okhttp3.** { *; }
-dontwarn okio.**

# ---------- Glide ----------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-dontwarn com.bumptech.glide.**

# Glide okhttp integration
-keep class com.bumptech.glide.integration.okhttp3.** { *; }

# ---------- ZXing ----------
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ---------- PhotoView / AndroidSVG / XBanner / MagicIndicator / SmartRefresh ----------
-dontwarn com.github.chrisbanes.photoview.**
-dontwarn com.caverock.androidsvg.**
-dontwarn com.github.xiaohaibin.**
-dontwarn net.lucode.hackware.magicindicator.**
-dontwarn com.scwang.smart.refresh.**

# ---------- CameraX ----------
-dontwarn androidx.camera.**
-keep class androidx.camera.** { *; }

# ---------- Datastore / Protobuf (javalite) ----------
# 保留 protobuf-lite 生成类以及解析器，防止消失
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**

# 保留实现 GeneratedMessageLite 的消息类（保守）
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite$Builder { *; }
-keepclassmembers class * {
    public static final ** DEFAULT_INSTANCE;
    public static ** parseFrom(...);
}

# ---------- Room (androidx.room) ----------
# 保留 Room 数据库类、Dao、Entity 及相应的构造/字段（防止注解处理生成的类被混淆）
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.RoomDatabase class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
# 保留 Room 自动生成的 processors 可能引用的字段名
-keepclassmembers class * {
    public static final **[] $VALUES;
}

# ---------- Walle (Meituan) 渠道包 ----------
-keep class com.meituan.android.walle.** { *; }
-dontwarn com.meituan.android.walle.**

# ---------- Skin support (skin.support) ----------
-keep class skin.support.** { *; }
-dontwarn skin.support.**

# ---------- Bugly ----------
-dontwarn com.tencent.bugly.**
-keep class com.tencent.bugly.** { *; }

# ---------- Chucker (debug only) ----------
-dontwarn com.chuckerteam.chucker.**
-keep class com.chuckerteam.chucker.** { *; }

# ---------- AspectJ (AOP) ----------
-dontwarn org.aspectj.**
-keep class org.aspectj.** { *; }
-keepattributes Annotation
-dontoptimize

# Keep possible aspect classes in project / aop utils
-keep class * implements org.aspectj.lang.annotation.* { *; }

# 保留注解切点定义与Around等注解成员
-keepclassmembers class * {
    @org.aspectj.lang.annotation.Pointcut *;
}
-keepclassmembers class * {
    @org.aspectj.lang.annotation.Around *;
}

# ---------- skin / project specific packages ----------
# 按你项目中已有包名保留关键模块（可根据需要再收窄）
-keeppackagenames com.dian.demo
-keep class com.dian.demo.di.model.** { *; }
-keep class com.dian.demo.di.vm.** { *; }
-keep class com.dian.demo.ui.activity.** { *; }
-keep class com.dian.demo.ui.img.ImageSelectActivity { *; }
-keep class com.dian.demo.utils.aop.** { *; }

# 你项目中 exturing 包的模型（你原来有）
-keep class com.exturing.uiagent.model.** { *; }

# ---------- 其它常用兼容规则 ----------
# 保留通过反射查找的 JS 接口方法（有 WebView JS 的话）
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
# 保留枚举 values/valueOf
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留注解标注的方法（你使用的自定义 AOP 注解）
-keepclassmembers class * {
    @com.dian.demo.utils.aop.SingleClick *;
}
-keepclassmembers class * {
    @com.dian.demo.utils.aop.CheckPermissions *;
}

# 保留可能被反射使用的类名/字段（通用安全网）
-keepnames class * {
    @androidx.annotation.Keep *;
}
-keepnames interface * { *; }

# 防止对这些常见 package 发出大量警告
-dontwarn javax.annotation.**
-dontwarn org.codehaus.**


-keep class kotlinx.coroutines.flow.** { *; }
-dontwarn kotlinx.coroutines.**


# 保留 layout 中的自定义控件
-keep class com.dian.demo.ui.view.** { *; }

# 保留 DataBinding / ViewBinding
-keep class **Binding { *; }
-keep class **BR { *; }
-keepclassmembers class * implements android.viewbinding.ViewBinding {
  public static * inflate(android.view.LayoutInflater);
  public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
  public static * bind(android.view.View);
}

# 保留 AppCompat Toolbar / ActionBar cast
-keepclassmembers class * extends androidx.appcompat.widget.Toolbar { *; }

# R 文件保留
-keep class **.R$* { *; }
-keepclassmembers class **.R$* { *; }

-keep class com.dian.demo.http.Result { *; }

# 保留 skin-support 相关类
-keep class skin.support.** { *; }
-dontwarn skin.support.**
-keepattributes *Annotation*

# 保留皮肤包内的资源名称（避免 R 文件被混淆）
-keepclassmembers class **.R$* {
    public static <fields>;
}



############################################################
# 结束说明：
# 1) 这份规则较为保守（优先保留运行时会用到的类），可大幅减少运行时因混淆造成的错误。
# 2) 若构建时仍报某些类/方法缺失（ClassNotFound/NoSuchMethodError/NoSuchFieldError），请贴出构建或运行时的完整错误日志（尤其是 R8 的 missing_rules / stacktrace），我会据此进一步收窄/定制规则（把不必要的 keep 变为更精确的 keep）。
# 3) 若你希望更激进缩小混淆范围（减小 APK 尺寸），我可以在拿到混淆后出现的问题列表时做二次精细化（针对性的 keep）。
############################################################
