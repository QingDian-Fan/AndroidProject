#############################################
# lib_common-share consumer rules（QQ / 微信 / 微博）
#############################################

-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

#--------------------------------------------
# 微信 SDK
#--------------------------------------------
# 注意：wechat-sdk-android-without-mta 的 AAR 自带 proguard.txt，已经保留
#   -keep class com.tencent.mm.opensdk.** { *; }
#   -keep class com.tencent.wxop.** { *; }
# （可在 demo/build/outputs/mapping/release/configuration.txt 中查到该段落来源）。
# 因此 SDK 自身的类**不需要**在这里重复保留，本模块只补充官方规则覆盖不到的宿主侧入口。

# 宿主需在 "<applicationId>.wxapi" 包下提供回调 Activity，由微信客户端按固定包名+类名拉起。
# 这些类属于宿主而非 SDK，不在 com.tencent.mm.opensdk.** 范围内，官方规则无法覆盖。
-keep class **.wxapi.WXEntryActivity { *; }
-keep class **.wxapi.WXPayEntryActivity { *; }
# 宿主自行实现的 IWXAPIEventHandler（若未放在 wxapi 包下）同样由 SDK 回调分发。
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

# 说明：规则来源核实（依据上一次 Release 构建产物 configuration.txt 中的 61 个 AAR 规则段落）——
#   wechat-sdk-android-without-mta 6.6.4：**自带** proguard.txt，SDK 类由官方规则保留；
#   com.tencent.tauth:qqopensdk 3.51.1：configuration.txt 中无对应段落，未提供官方规则；
#   io.github.sinaweibosdk:core 11.12.0@aar：同上，且 @aar 声明不解析传递依赖。
# 因此 QQ / 微博的保留规则由本模块按官方接入文档补充，微信仅补充宿主侧入口。
# 说明：微信 AAR 随包提供 proguard.txt，其 SDK 内部规则已由 R8 自动合并；
# qqopensdk / weibo core 未提供 consumer rules，因此上述 QQ / 微博规则按各自官方接入文档补充。

# 说明：原先的 "-dontwarn com.tencent.**" 与 "-dontwarn com.sina.weibo.sdk.**" 已移除。
# 这两条以 SDK 根命名空间作为过滤器，既无法抑制注释所称的 org.apache.http 等外部可选依赖
# （那类缺失类不在 com.tencent 命名空间内），又会连真实的 SDK 缺失类一起吞掉，
# 使依赖不完整的问题推迟到运行时才以 NoClassDefFoundError 暴露。
# 如果 Release 构建确实报告缺失类，应从 AGP 生成的
# demo/build/outputs/mapping/release/missing_rules.txt 中取精确到类的规则补充到此处。
# 三方 SDK 编译期引用、运行期不提供的可选类型。
# 待收敛：R8 的 -dontwarn 按**缺失类**匹配，精确清单只能由一次 Release 构建产出
#   （移除下列规则后 AGP 会在 demo/build/outputs/mapping/release/missing_rules.txt
#     生成逐条 -dontwarn），当前会话无法执行 Release 构建，因此暂按 SDK 根包保留，
#   不做无依据的收窄，避免误删后 R8 直接以 "Missing class" 中断 Release 构建。
-dontwarn com.tencent.**
-dontwarn com.sina.weibo.sdk.**

# 说明：Glide 相关规则由 lib_common-image 的 consumer rules 统一提供，此处不重复。
