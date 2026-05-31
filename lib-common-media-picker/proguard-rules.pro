-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Activities and adapters are public API of this picker module.
-keep class com.common.media.picker.** { *; }

# Kotlin Parcelize / Parcelable creators.
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ViewBinding reflection from common-ui.
-keep class **.databinding.*Binding { *; }

-dontwarn com.github.chrisbanes.photoview.**
