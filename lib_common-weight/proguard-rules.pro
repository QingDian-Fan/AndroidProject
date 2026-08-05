#############################################
# lib_common-weight consumer rules（自定义 View / WebView JSBridge）
#############################################

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

#--------------------------------------------
# XML 布局实例化自定义 View
#--------------------------------------------
# LayoutInflater 按类名反射调用 (Context, AttributeSet) 构造方法。
# 类名来自 XML 字符串，因此本模块被 XML 引用的 View 必须保留类名与构造方法。
-keep class com.common.weight.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
}

#--------------------------------------------
# WebView JSBridge
#--------------------------------------------
# @JavascriptInterface 方法由 JS 按方法名调用，方法名不能被混淆。
# 依据：BaseWebView.kt 的 addJavascriptInterface(this, "webview") 与 takeNativeAction()。
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# JS 投递的 payload 由 Gson 按字段名解析（BaseWebView 中 gson.fromJson(jsParam, JsParam::class.java)），
# 字段名与无参/主构造函数不能被改写。
# 依据：webview/bean/JsParam.kt
-keep class com.common.weight.webview.bean.JsParam { *; }
# WebDataEntry 由 MoshiUtil 反射序列化到 DataStore（webview/storage/WebPageStore.kt）。
-keep class com.common.weight.webview.bean.WebDataEntry { *; }

# 命令分发按 name 字符串查找已注册的 Command，注册表是运行期 Map，
# Command 实现类允许混淆；仅保留接口方法签名以保证宿主自定义 Command 的二进制兼容。
# 依据：webview/command/Command.kt、webview/dispatcher/WebCommandRegistry.kt
-keep interface com.common.weight.webview.command.Command { *; }
-keep interface com.common.weight.webview.command.CommandCallback { *; }
-keep interface com.common.weight.webview.command.CommandBridge { *; }

# 视频播放引擎扩展点：宿主实现 VideoPlayerEngine 并通过工厂注入。
# 依据：video/VideoPlayerEngine.kt、video/VideoPlayerEngineFactory.kt
-keep interface com.common.weight.video.VideoPlayerEngine { *; }
-keep interface com.common.weight.video.VideoPlayerEngineFactory { *; }

# 说明：Media3/ExoPlayer 的 AAR 自带 consumer rules，不在此重复保留其类；
# 仅抑制其对可选解码/分片模块的编译期引用告警。
-dontwarn androidx.media3.**
