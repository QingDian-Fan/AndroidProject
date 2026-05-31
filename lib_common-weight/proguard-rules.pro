-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# XML inflated custom views.
-keep class com.common.weight.** extends android.view.View { public <init>(...); }
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

# WebView JS bridge and JSON payload models.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.common.weight.webview.webview.BaseWebView {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.common.weight.webview.bean.** { *; }
-keep class com.common.weight.webview.command.** { *; }
-keep class com.common.weight.webview.dispatcher.** { *; }

# Media3/ExoPlayer publishes its own rules; suppress optional references.
-dontwarn androidx.media3.**
