# 需求：项目全模块混淆配置与 Release 验证-bug

## 需求背景

P1
APT 登录判断类被 R8 整类裁剪
文件：[demo/proguard-rules.pro (line 55)](/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject/demo/proguard-rules.pro:55)
代码位置：55–58 行。
问题描述：-keepnames 只阻止改名，不阻止裁剪；-keepclassmembers 也不会让仅由字符串引用的宿主类保持可达。最终 [usage.txt (line 27484)](/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject/demo/build/outputs/mapping/release/usage.txt:27484) 明确包含整个 com.demo.project.auth.LoginState，而 mapping.txt 中不存在该类。
触发条件：LoginHookUtil.isGeneratedLogin() 根据 AndLoginUtils.getJudgeLoginMethod() 返回的 "com.demo.project.auth.LoginState#isLogin" 执行反射。
实际影响：Class.forName() 必然失败并返回未登录，@CheckLogin 生成链路无法完成验收。当前 LoginState.isLogin() 恰好委托给 AuthManager.isLogin()，因此部分场景表面行为可能一致，但反射契约已经失效。
最小修复建议：改为精确的 -keep class com.demo.project.auth.LoginState { public static boolean isLogin(); }，重新构建后确认该类不再出现在 usage.txt，且方法保留在 mapping.txt/seeds.txt。

Cookie 自定义序列化方法被裁剪
文件：[lib_common-http/proguard-rules.pro (line 24)](/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject/lib_common-http/proguard-rules.pro:24)、[OkHttpCookies.kt (line 26)](/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject/lib_common-http/src/main/java/com/common/http/cookie/OkHttpCookies.kt:26)
代码位置：writeObject() 第 26 行、readObject() 第 39 行；consumer rules 中没有对应规则。
问题描述：R8 的 usage.txt 明确列出 readObject/writeObject 已被移除；[mapping.txt (line 274575)](/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject/demo/build/outputs/mapping/release/mapping.txt:274575) 中混淆后的 OkHttpCookies 只剩 cookies 字段。okhttp3.Cookie 本身不实现 Serializable。
触发条件：网络响应包含 Cookie，PersistentCookieStore.encodeCookie() 调用 ObjectOutputStream.writeObject()。
实际影响：缺少自定义 writeObject() 后会退化为默认序列化，并因 Cookie 不可序列化产生 NotSerializableException；异常被吞掉并返回 null，导致 Cookie 无法持久化，进程重启后登录态可能丢失。
最小修复建议：对单个 OkHttpCookies 类添加精确完整保留规则，例如 -keep class com.common.http.cookie.OkHttpCookies { *; }。这样同时保护类名、序列化钩子和跨版本默认 serialVersionUID 稳定性。

P2
微信 SDK 字节码持续产生 R8 警告
文件：[lib_common-share/build.gradle (line 47)](/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject/lib_common-share/build.gradle:47)
代码位置：微信 SDK 6.6.4 依赖。
问题描述：本次 Release 日志出现 132 条 Expected stack map table for method with non-linear control flow。
触发条件：每次 Release R8 处理微信 SDK。
实际影响：当前 R8 仍构建成功，但警告明确提示后续 R8 版本可能把相关方法视为不可达，存在分享能力升级后失效风险。
最小修复建议：本期禁止升级依赖时记录为接受风险，不要用 -dontwarn 掩盖；后续验证包含正确栈映射表的官方 SDK 版本并完成分享回归。

微信 consumer rules 的现状说明不准确且存在重复规则
文件：[lib_common-share/proguard-rules.pro (line 17)](/Users/dian/AndroidStudioProjets/DemoProjects/GitHubProjects/AndroidProject/lib_common-share/proguard-rules.pro:17)
代码位置：17–19、47–52 行。
问题描述：微信 6.6.4 AAR 实际自带 proguard.txt，完整保留 com.tencent.mm.opensdk.**；本地规则重复保留其子包，但注释声称微信 SDK 没有 consumer rules。同时 -dontwarn com.tencent.** 范围大于注释描述的可选依赖。
触发条件：最终 R8 合并 Library 与第三方 AAR consumer rules。
实际影响：当前主要是重复保留和潜在告警掩盖，违反“优先使用官方规则、精确 dontwarn”的验收原则。
最小修复建议：保留宿主 WXEntryActivity 和宿主回调规则，删除已由官方规则覆盖的 SDK 内部子包规则；将 dontwarn 收敛到实际确认的缺失类型。