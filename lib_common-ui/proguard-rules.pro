#############################################
# lib_common-ui consumer rules（ViewBinding 反射基类）
#############################################
# 依据：ViewBindingReflect.kt
#   1) findBindingClass() 沿 genericSuperclass 解析 B : ViewBinding —— 依赖 Signature 属性；
#   2) bindingClass.getMethod("bind", View::class.java) —— 依赖 bind 方法名不被混淆。

-keepattributes Signature,InnerClasses,EnclosingMethod

# 泛型基类不能被 R8 的类合并（vertical merging）折叠掉，
# 否则子类的 genericSuperclass 不再是 ParameterizedType，findBindingClass() 会抛异常。
# 允许改名，只要求类结构保留。
-keep,allowobfuscation class com.common.ui.BaseAppBindActivity
-keep,allowobfuscation class com.common.ui.BaseAppVMActivity
-keep,allowobfuscation class com.common.ui.BaseAppBindFragment
-keep,allowobfuscation class com.common.ui.BaseAppVMFragment

# ViewBinding 生成类可能只被泛型签名引用（页面未直接访问 binding 字段时），
# 这种引用不构成可达性，需显式防止被 shrink 掉；类名允许混淆。
-keep,allowobfuscation class * implements androidx.viewbinding.ViewBinding

# ViewBinding 生成类的静态工厂方法名按字符串反射调用，必须保留方法名。
# 类名本身可以混淆：Class 对象来自泛型签名，不来自字符串。
-keepclassmembers class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
    public static *** inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
