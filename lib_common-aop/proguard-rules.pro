#############################################
# lib_common-aop consumer rules（AspectJ 切面）
#############################################
# 依据：src/main/java/com/common/aop/ 下的 5 个 @Aspect 类与 3 个 RUNTIME 注解。
# 织入由 gradle/aspectj-weave.gradle 在 R8 之前完成，R8 处理的是织入后的 class。

# 切面在运行期通过 MethodSignature.getMethod().getAnnotation(...) 读取切点注解，
# 需要 RuntimeVisibleAnnotations；Signature/InnerClasses/EnclosingMethod 供 Kotlin 元数据与内部类使用。
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,AnnotationDefault

# 切点注解类型本身。被裁剪后 getAnnotation() 返回 null，切面逻辑（防抖时长、权限列表等）失效。
# 依据：SingleClick.kt / CheckNet.kt / CheckPermissions.kt 均为 AnnotationRetention.RUNTIME。
-keep @interface com.common.aop.SingleClick
-keep @interface com.common.aop.CheckNet
-keep @interface com.common.aop.CheckPermissions

# 被上述注解标记的方法：织入后原方法体被移入 ajc 生成的方法，
# 但切面仍按注解查找，方法签名不能被裁剪掉。
-keepclassmembers class * {
    @com.common.aop.SingleClick <methods>;
    @com.common.aop.CheckNet <methods>;
    @com.common.aop.CheckPermissions <methods>;
}

# ajc 为 @Aspect 类生成的 aspectOf()/hasAspect() 静态方法：
# 只被织入后的字节码引用，源码中无调用方。允许类名混淆，仅保留这两个入口方法名。
# org.aspectj.lang.annotation.Aspect 为 RUNTIME 保留，可被 R8 匹配。
-keepclassmembers @org.aspectj.lang.annotation.Aspect class * {
    public static *** aspectOf();
    public static boolean hasAspect();
}

# around advice 生成的闭包类：由织入字节码 new 出来并传给 proceed()，
# 名称形如 XxxAspect$AjcClosure1，源码中无引用。
-keep class **AjcClosure* { *; }

# ajc 生成的织入辅助成员：ajc$tjp_* 静态切点字段、ajc$preClinit()、ajc$xxx 包装方法等。
-keepclassmembers class * {
    *** ajc$*(...);
    *** ajc$*;
}

# aspectjrt 中面向 JVM agent / LTW 的可选引用（java.lang.instrument 等）在 Android 上不存在。
# 仅抑制 aspectj 运行时自身的告警，不掩盖织入失败（织入失败会在 ajc 阶段直接报 error）。
-dontwarn org.aspectj.**
