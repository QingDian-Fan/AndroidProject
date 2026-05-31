-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

-keep @org.aspectj.lang.annotation.Aspect class * { *; }
-keep class com.common.aop.** { *; }
-keep class **AjcClosure* { *; }
-keepclassmembers class * {
    *** ajc$*(...);
}
-dontwarn org.aspectj.**
