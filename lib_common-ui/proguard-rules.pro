-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# BaseAppBindActivity/BaseAppBindFragment reflect generated ViewBinding classes.
# Their subclasses must keep generic superclass signatures, otherwise
# ViewBindingReflect cannot resolve B : ViewBinding after R8 optimization.
-keep class **.databinding.*Binding { *; }
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * bind(android.view.View);
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}
-keep class * extends com.common.ui.BaseAppBindActivity { *; }
-keep class * extends com.common.ui.BaseAppVMActivity { *; }
-keep class * extends com.common.ui.BaseAppBindFragment { *; }
-keep class * extends com.common.ui.BaseAppVMFragment { *; }
-keep class * extends com.common.ui.skin.BaseSkinBindActivity { *; }
-keep class * extends com.common.ui.skin.BaseSkinVMActivity { *; }
-keep class * extends com.common.ui.skin.BaseSkinBindFragment { *; }
-keep class * extends com.common.ui.skin.BaseSkinVMFragment { *; }

# Public UI base classes and skin enums can be referenced from host apps.
-keep class com.common.ui.BaseActivity { *; }
-keep class com.common.ui.BaseAppBindActivity { *; }
-keep class com.common.ui.BaseAppVMActivity { *; }
-keep class com.common.ui.BaseFragment { *; }
-keep class com.common.ui.BaseAppBindFragment { *; }
-keep class com.common.ui.BaseAppVMFragment { *; }
-keep class com.common.ui.ViewBindingReflect { *; }
-keep class com.common.ui.skin.BaseSkinBindActivity { *; }
-keep class com.common.ui.skin.BaseSkinVMActivity { *; }
-keep class com.common.ui.skin.BaseSkinBindFragment { *; }
-keep class com.common.ui.skin.BaseSkinVMFragment { *; }
-keep class com.common.ui.skin.Skin { *; }
-keep class com.common.ui.skin.SkinNightMode { *; }
-keep class com.common.ui.skin.Language { *; }
