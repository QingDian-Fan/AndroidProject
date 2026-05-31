-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# BaseAppBindActivity/BaseAppBindFragment reflect generated ViewBinding classes.
-keep class **.databinding.*Binding { *; }
-keep class * implements androidx.viewbinding.ViewBinding {
    public static * bind(android.view.View);
    public static * inflate(android.view.LayoutInflater);
    public static * inflate(android.view.LayoutInflater, android.view.ViewGroup, boolean);
}

# Public UI base classes and skin enums can be referenced from host apps.
-keep class com.common.ui.BaseActivity { *; }
-keep class com.common.ui.BaseAppBindActivity { *; }
-keep class com.common.ui.BaseFragment { *; }
-keep class com.common.ui.BaseAppBindFragment { *; }
-keep class com.common.ui.ViewBindingReflect { *; }
-keep class com.common.ui.skin.Skin { *; }
-keep class com.common.ui.skin.SkinNightMode { *; }
-keep class com.common.ui.skin.Language { *; }
