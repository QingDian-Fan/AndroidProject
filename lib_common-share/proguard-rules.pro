#############################################
# lib_common-share consumer rules（QQ / 微信 / 微博）
#############################################

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

#--------------------------------------------
# 微信 SDK
#--------------------------------------------
# 微信要求宿主在 "<applicationId>.wxapi" 包下提供 WXEntryActivity / WXPayEntryActivity，
# 由微信客户端按固定包名+类名拉起，类名不能被改写。
-keep class **.wxapi.WXEntryActivity { *; }
-keep class **.wxapi.WXPayEntryActivity { *; }
# IWXAPIEventHandler 回调由 SDK 反射分发。
-keep class * implements com.tencent.mm.opensdk.openapi.IWXAPIEventHandler { *; }
# 微信 SDK 内部通过反射构造 modelmsg / modelbase 下的请求响应对象。
-keep class com.tencent.mm.opensdk.modelmsg.** { *; }
-keep class com.tencent.mm.opensdk.modelbase.** { *; }
-keep class com.tencent.mm.opensdk.openapi.** { *; }

#--------------------------------------------
# QQ / 腾讯开放平台 SDK
#--------------------------------------------
# tauth 通过反射回调 IUiListener；Assist 相关类由 SDK 内部按名加载。
-keep class com.tencent.tauth.** { *; }
-keep class * implements com.tencent.tauth.IUiListener { *; }
-keep class com.tencent.open.** { *; }
-keep class com.tencent.connect.** { *; }

#--------------------------------------------
# 微博 SDK
#--------------------------------------------
# 微博 SDK 按类名反射回调授权与分享结果。
-keep class com.sina.weibo.sdk.** { *; }

#--------------------------------------------
# 本模块对外入口
#--------------------------------------------
# 分享回调接口由宿主实现并由本模块回调，方法名构成二进制契约。
# 依据：ShareCallBack.java、channel/Channel.java
-keep interface com.common.share.ShareCallBack { *; }
-keep interface com.common.share.channel.Channel { *; }
# ShareModel 在 Activity 之间通过 Intent 传递并被 SDK 读取字段。
-keepclassmembers class com.common.share.ShareModel { *; }
# QQShareListener 实现 IUiListener，已被上面的 implements 规则覆盖，此处不重复。

# 说明：三方 SDK 均未随 AAR 提供完整 consumer rules（qqopensdk / weibo core / wechat-sdk
# 属于 @aar 或无规则发布），因此上述规则按各 SDK 官方接入文档要求补充。

# 三方 SDK 编译期引用的可选依赖（如 QQ SDK 的 org.apache.http、微博的可选组件）。
-dontwarn com.tencent.**
-dontwarn com.sina.weibo.sdk.**

# 说明：Glide 相关规则由 lib_common-image 的 consumer rules 统一提供，此处不重复。
