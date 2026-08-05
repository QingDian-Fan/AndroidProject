#############################################
# lib_common-scan consumer rules（OpenCV JNI / 扫码）
#############################################

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

#--------------------------------------------
# OpenCV Java API（JNI）
#--------------------------------------------
# org.opencv 下的 Java 类是 libopencv_java4.so 的对应封装：
#   1) 大量 n_xxx native 方法由 C++ 按 "Java_org_opencv_..." 规则静态注册，类名与方法名必须完全一致；
#   2) Mat/Size/Rect 等对象的 nativeObj 字段由 C++ 直接按字段名读写。
# 因此整包保留是 JNI 符号映射的硬性要求。
-keep class org.opencv.** { *; }
# OpenCV 对可选模块（如 java.awt、部分未打包的算法模块）存在编译期引用。
-dontwarn org.opencv.**

# 所有 native 方法及其所属类都不能被改名，否则找不到 JNI 符号。
-keepclasseswithmembernames class * {
    native <methods>;
}

#--------------------------------------------
# 本模块对外入口
#--------------------------------------------
# 扫码结果回调由宿主实现，方法名构成二进制契约。
# 依据：camera/CameraScan.java 的 OnScanResultCallback、analyze/Analyzer.java
-keep interface com.common.scan.camera.CameraScan$OnScanResultCallback { *; }
-keep interface com.common.scan.camera.analyze.Analyzer { *; }
-keep interface com.common.scan.camera.analyze.Analyzer$OnAnalyzeListener { *; }
# CameraScan.parseScanResult(Intent) / SCAN_RESULT 常量是宿主取结果的固定入口。
-keepclassmembers class com.common.scan.camera.CameraScan {
    public static <fields>;
    public static <methods>;
}
# AnalyzeResult 由回调传给宿主并按属性读取。
-keepclassmembers class com.common.scan.camera.AnalyzeResult { *; }

# 说明：扫码 Activity（WeChatQRCodeActivity 等）由本模块 Manifest 注册，
# AGP 会自动生成对应 keep 规则；相机配置、ViewfinderView 等内部实现无反射入口，
# 允许 R8 裁剪、优化与改名，因此移除原来的 com.common.scan.** 整包 keep。
