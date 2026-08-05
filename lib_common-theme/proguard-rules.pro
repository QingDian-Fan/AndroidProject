#############################################
# lib_common-theme consumer rules
#############################################
# 本模块只有 BaseApplication 与日夜间/多语言管理对象，无反射、无 JNI、无生成代码。

# BaseApplication 由宿主 Manifest 的 android:name 间接引用（宿主 Application 继承它），
# Manifest 组件由 AGP 自动生成的规则保留；这里仅保留公共属性元数据。
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# 说明：NightModeManager / AppLanguageManager 通过 AppCompatDelegate 与 DataStore 工作，
# 不依赖类名或方法名字符串，允许 R8 完整裁剪、优化与改名，故不添加 keep 规则。
