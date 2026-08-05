#############################################
# demo（Application）R8 规则
#############################################
# 职责边界：本文件只负责 Application 自身的入口、业务反射与最终集成兜底。
# 各 common 模块的通用规则由其 consumerProguardFiles 随 AAR 传入，此处不重复维护。

#--------------------------------------------
# 通用属性
#--------------------------------------------
# Release 崩溃定位需要行号；-renamesourcefileattribute 隐藏真实源码文件名。
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 泛型签名（ViewBinding 反射 / Retrofit / Moshi）、运行期注解（AOP 切点）、
# 内部类与外围方法（Kotlin 匿名类、companion）。
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,AnnotationDefault

#--------------------------------------------
# Scheme 字符串路由
#--------------------------------------------
# SchemeUtils.navigateActivity() 使用 SchemeConstants 中的完整类名字符串
# Class.forName(path)，再从 declaredMethods / Companion 中挑选名为 "start" 的方法反射调用。
# R8 不改写字符串字面量，因此这些类的**类名**与 start 入口必须固定。
# 依据：utils/scheme/SchemeConstants.kt、utils/scheme/SchemeUtils.kt
-keepnames class com.demo.project.ui.activity.SplashActivity
-keepnames class com.demo.project.ui.activity.HomeActivity
-keepnames class com.demo.project.ui.activity.LoginActivity
-keepnames class com.demo.project.ui.activity.WebActivity
-keepnames class com.demo.project.ui.activity.WebExplorerActivity
-keepnames class com.demo.project.ui.activity.CameraActivity
-keepnames class com.demo.project.ui.activity.VideoPlayerActivity
-keepnames class com.demo.project.ui.activity.DebugActivity

# 反射按方法名 "start" 查找入口；@JvmStatic 会同时生成宿主类静态方法与
# Companion 实例方法，两者都可能被选中，因此一并保留。
-keepclassmembers class com.demo.project.ui.activity.** {
    public static void start(android.content.Context, ...);
    public void start(android.content.Context, ...);
    public static ** Companion;
}

# lib-common-media-picker 的 ImagePreviewActivity 反射调用
# VideoPlayerActivity.start(Context, String)（@JvmStatic），已由上面的规则覆盖。

#--------------------------------------------
# APT 登录链路
#--------------------------------------------
# KAPT 生成的 com.dian.demo.apt.AndLoginUtils 由 lib_common-auth 的 consumer rules 保留。
# 但其方法体返回的是**字符串常量**，R8 不会改写字符串，因此字符串指向的目标必须固定名称：
#   getLoginActivity()     -> "com.demo.project.ui.activity.LoginActivity"（已在上面 -keepnames）
#   getJudgeLoginMethod()  -> "com.demo.project.auth.LoginState#isLogin"
#   getRequireLoginList()  -> 当前工程无 @RequireLogin 标注，返回空列表
# 依据：lib_processor/RequireLoginProcessor.java 的 createXxxFun()、
#      lib_common-auth/hook/LoginHookUtil.java 的 isGeneratedLogin()
-keepnames class com.demo.project.auth.LoginState
-keepclassmembers class com.demo.project.auth.LoginState {
    public static boolean isLogin();
}

# 注意：@RequireLogin 为 RetentionPolicy.SOURCE，且 lib_annotation 在本模块是 compileOnly，
# 注解类不进入 APK。因此不能用 "-keep @com.dian.annotation.RequireLogin class *" 保护目标页面；
# 新增受保护页面时，需要在此处按类名补充 -keepnames。

#--------------------------------------------
# 枚举字符串
#--------------------------------------------
# AudioPlayerActivity 通过 Intent 传 AudioEngineType.name，再用 valueOf(String) 还原，
# 需要保留枚举常量名并阻止 R8 的 enum unboxing 优化。
# 依据：player/audio/AudioEngineType.kt、ui/activity/AudioPlayerActivity.kt
-keepclassmembers enum com.demo.project.player.audio.AudioEngineType {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

#--------------------------------------------
# 集成兜底
#--------------------------------------------
# 说明：CustomProjectLiveData 反射的是 androidx.lifecycle.LiveData 的私有字段 mObservers
# 与其 "get" 方法，目标为 AndroidX 类（由 androidx 自带 consumer rules 覆盖），
# 且反射对 R8 不可见、不会产生告警，因此无需在此添加规则。

# 说明：以下规则不在本文件维护，由对应模块的 consumer rules 提供——
#   ViewBinding 反射            -> lib_common-ui
#   Retrofit / Room / Moshi     -> lib_common-http、lib_common-utils
#   AspectJ 切面与织入产物      -> lib_common-aop
#   WebView JSBridge / 自定义 View -> lib_common-weight
#   Glide / Coil                -> lib_common-image
#   QQ / 微信 / 微博 SDK        -> lib_common-share
#   OpenCV JNI / 扫码           -> lib_common-scan
#   FFmpeg JNI                  -> lib_common-player
#   Parcelable / 图片选择入口   -> lib-common-media-picker
