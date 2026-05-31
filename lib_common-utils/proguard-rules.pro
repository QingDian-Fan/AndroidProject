-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Moshi reflective adapter helpers use Kotlin reflection metadata.
-keep class com.common.utils.moshi.** { *; }
-keep class com.common.utils.ext.Stroke { *; }

# LoginHookUtil reflects these annotations and generated APT helper methods.
-keep class com.dian.demo.apt.** { *; }
-keep @com.dian.annotation.LoginPage class * { *; }
-keep @com.dian.annotation.RequireLogin class * { *; }
-keepclassmembers class * {
    @com.dian.annotation.CheckLogin <methods>;
}

-dontwarn javax.annotation.**
