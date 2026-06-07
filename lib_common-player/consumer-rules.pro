#############################################
# lib_common-player (FFmpeg + JNI) consumer rules
#############################################
# 该模块通过 JNI 与 C++ 交互：
#  1) nativeXxx 系列为 native 方法，方法名需与 C++ 注册保持一致；
#  2) onNativeXxx 系列是 C++ 通过 GetMethodID 按方法名回调的 Java 方法，
#     在 Java 侧无调用方，开启 R8 后会被裁剪/重命名导致回调失败。
# 因此整体保留该包，类名与成员名均不混淆。
-keep class com.common.player.** { *; }
-keepclassmembers class com.common.player.** {
    native <methods>;
    void onNative*(...);
}
