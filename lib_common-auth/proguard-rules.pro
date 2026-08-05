#############################################
# lib_common-auth consumer rules（APT 登录链路反射）
#############################################
# 依据：hook/LoginHookUtil.java
#   Class.forName(AuthConstants.DEFAULT_GENERATED_HELPER) = "com.dian.demo.apt.AndLoginUtils"
#   clazz.getMethod("getLoginActivity" / "getRequireLoginList" / "getJudgeLoginMethod")

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# KAPT 生成的登录辅助类：类名写死在 AuthConstants 中按字符串反射查找，
# 三个静态方法同样按方法名反射调用，类名与方法名都不能被改写。
-keep class com.dian.demo.apt.AndLoginUtils {
    public static java.lang.String getLoginActivity();
    public static java.util.List getRequireLoginList();
    public static java.lang.String getJudgeLoginMethod();
}

# 说明：AndLoginUtils 内部是字符串常量（登录页类名、需登录页面类名、"类名#方法名"），
# R8 不会改写字符串字面量，因此这些字符串指向的目标类/方法必须由**宿主模块**
# 用 -keepnames 固定名称（见 demo/proguard-rules.pro）。本模块无法预知宿主类名，
# 不在此处添加规则。

# 说明：com.dian.annotation 下的 @RequireLogin 为 RetentionPolicy.SOURCE；
# @LoginPage / @CheckLogin 虽为 RUNTIME，但 lib_annotation 在 demo 中以 compileOnly 引入，
# 注解类不会进入 APK。因此基于这些注解的 R8 匹配规则
# （-keep @com.dian.annotation.X class *）在本项目中恒不生效，已移除，
# 避免留下表面存在但实际无效的配置。

# 说明：LoginHookUtil 对 android.app.ActivityTaskManager / IActivityManager /
# android.util.Singleton 的反射目标均为 Android Framework 类，
# 不需要保留本模块自身的业务类，因此不添加 com.common.auth.** 整包 keep。
