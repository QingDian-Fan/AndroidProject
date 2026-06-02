-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# OpenCV Java classes contain many JNI entry points whose names must remain
# stable for libopencv_java4.so.
-keep class org.opencv.** { *; }
-keep class com.common.scan.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-dontwarn org.opencv.**
