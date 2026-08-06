# 需求：项目全模块混淆配置与 Release 验证-Bug

## 需求背景

### P1 高

#### 1. AspectJ 织入失败不会终止 Release 构建

- 严重等级：P1 高
- 文件路径：[gradle/aspectj-weave.gradle](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/gradle/aspectj-weave.gradle:72)
- 代码位置：72–89 行
- 问题描述：`Main.run()` 将织入结果写入 `MessageHandler`，但脚本遇到 `IMessage.ABORT`、`ERROR` 或 `FAIL` 时只调用 `project.logger.error()`，没有抛出 `GradleException` 或以其他方式令任务失败。此次实现也未修正该验收链路。
- 触发条件：Release 编译期间出现 AspectJ 类型解析、字节码版本、classpath 或织入错误。
- 实际影响：Gradle 可以继续执行 R8 并产出 APK，但相关调用点可能仍是未织入或部分织入的 class。`@SingleClick`、`@CheckNet`、`@CheckPermissions` 等功能可能静默失效，与“不得出现 AspectJ error、abort 或 weave failure”以及 Release 必须使用织入后 class 的要求直接冲突。
- 修复建议：记录消息后检查是否存在 `ABORT/ERROR/FAIL`，存在时抛出 `GradleException` 终止对应编译任务；同时保留 weave info 供 Release 验证。

### P2 中

#### 1. `-dontwarn` 范围与注释所述可选依赖不匹配

- 严重等级：P2 中
- 文件路径：
  - [lib_common-share/proguard-rules.pro](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/lib_common-share/proguard-rules.pro:47)
  - [lib_common-weight/proguard-rules.pro](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/lib_common-weight/proguard-rules.pro:47)
- 代码位置：share 47–52 行；weight 47–49 行
- 问题描述：
  - `-dontwarn com.tencent.**`、`-dontwarn com.sina.weibo.sdk.**` 抑制的是这些命名空间中的缺失类，并不能针对注释所说的 `org.apache.http` 等外部可选依赖。
  - `-dontwarn androidx.media3.**` 会屏蔽整个 Media3 命名空间的缺失类，而不是精确限定某个可选解码或分片组件。
- 触发条件：SDK 依赖不完整、版本发生冲突，或 Release R8 遇到真正缺失的腾讯、微博、Media3 类。
- 实际影响：真正的依赖缺失可能被静默忽略并延后为运行时 `NoClassDefFoundError`；与此同时，注释声称要处理的外部可选依赖仍可能继续产生告警。不符合“每条 `-dontwarn` 对应确定可选依赖、不得掩盖真实缺失依赖”的要求。
- 修复建议：先根据实际 R8 Missing class 输出确认缺失目标，只为明确的可选类添加精确规则；不存在告警时删除这些规则。不要使用整个 SDK 根命名空间作为告警过滤器。

#### 2. 重复维护微信 SDK 已自带的整包保留规则

- 严重等级：P2 中
- 文件路径：[lib_common-share/proguard-rules.pro](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/lib_common-share/proguard-rules.pro:17)
- 代码位置：17–19 行、47–48 行
- 问题描述：规则注释声称微信 SDK 未随 AAR 提供完整 consumer rules，但本机解析的 `wechat-sdk-android-without-mta-6.6.4.aar` 内含 `proguard.txt`，其中已经保留 `com.tencent.mm.opensdk.**` 和 `com.tencent.wxop.**`。当前规则再次手工保留其子包，与“优先采用第三方官方 consumer rules、不做无意义重复”的目标冲突。
- 触发条件：所有包含 `lib_common-share` 的 Release R8 构建。
- 实际影响：当前版本形成重复配置和错误的规则依据；SDK 升级后，手工副本还可能与官方规则发生偏差，使审计无法判断实际规则来源。
- 修复建议：删除已由微信 AAR 提供的重复规则，仅保留宿主自行实现且官方规则无法覆盖的回调 Activity/接口规则；分别核实 QQ、微博当前制品后再决定是否需要补充。

