-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# Moshi reflective adapter helpers use Kotlin reflection metadata.
-keep class com.common.utils.moshi.** { *; }
-keep class com.common.utils.ext.Stroke { *; }

-dontwarn javax.annotation.**
