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
# IWXAPIEventHandler 由宿主实现，回调由 SDK 反射分发，宿主实现类不在 SDK 包内，需自行保留。
-keep class * implements com.tencent.mm.opensdk.openapi.IWXAPIEventHandler { *; }
# 说明：SDK 内部的 modelmsg / modelbase / openapi 等包由微信 AAR 自带的 proguard.txt
# 以 "-keep class com.tencent.mm.opensdk.** { *; }"（含 com.tencent.wxop.**）完整覆盖，
# 本文件不重复维护，避免官方规则升级后出现两份不一致的保留范围。

# 已知风险（本期接受，不做依赖升级、也不用 -dontwarn 掩盖）：
# wechat-sdk-android-without-mta:6.6.4 的部分字节码缺少栈映射表，Release R8 会输出
# "Expected stack map table for method with non-linear control flow" 告警（本次 132 条）。
# 当前 R8 仍构建成功，但后续 R8 版本可能把相关方法判定为不可达。
# 后续处理：升级到带正确栈映射表的官方 SDK 版本，并完成分享链路回归。

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

# 说明：微信 AAR 随包提供 proguard.txt，其 SDK 内部规则已由 R8 自动合并；
# qqopensdk / weibo core 未提供 consumer rules，因此上述 QQ / 微博规则按各自官方接入文档补充。

# 三方 SDK 编译期引用、运行期不提供的可选类型。
# 待收敛：R8 的 -dontwarn 按**缺失类**匹配，精确清单只能由一次 Release 构建产出
#   （移除下列规则后 AGP 会在 demo/build/outputs/mapping/release/missing_rules.txt
#     生成逐条 -dontwarn），当前会话无法执行 Release 构建，因此暂按 SDK 根包保留，
#   不做无依据的收窄，避免误删后 R8 直接以 "Missing class" 中断 Release 构建。
-dontwarn com.tencent.**
-dontwarn com.sina.weibo.sdk.**

# 说明：Glide 相关规则由 lib_common-image 的 consumer rules 统一提供，此处不重复。
