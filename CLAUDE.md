# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

本项目是以 `demo` 组合 common library 的 Kotlin 为主多模块 Android 工程。Claude Code 在终端中启动时会读取本文件作为项目级记忆。

## 1. Claude Code 的工作模式

Claude Code 在本项目中有两种模式。

### 1.1 审查模式

当用户、自动化脚本或提示词出现以下任一内容时，进入审查模式：

- “代码审查”“review”“审查 Codex”。
- 指定基准提交、`BASE_COMMIT` 或要求检查 Git Diff。
- 要求检查当前工作区、未提交修改或某个提交范围。
- 明确要求“只读”“不得修改文件”。

审查模式下：

- 只读取、搜索、分析和运行不会修改源码的检查命令。
- 不得修改、格式化、生成、删除或移动项目文件，也不得自动修复发现的问题。
- 不得执行 `git add`、`git commit`、`git reset`、`git restore`、`git checkout` 或 `git clean`。
- 审查结论必须独立形成，不能复述或相信 Codex 的自我评价。
- 只报告具有明确代码依据、触发条件和实际影响的问题。

### 1.2 实现模式

只有用户明确要求 Claude Code 编写、修改或修复代码时，才进入实现模式。

实现模式下：

- 遵守本文件的项目结构、模块边界、编码、安全和验证规则。
- 不覆盖用户已有未提交改动。
- 修改后执行适用的 Gradle 构建或测试。
- 不主动提交、推送、切换分支或清理工作区。

当模式不明确时，优先保持只读，先分析当前代码和 Git 状态。

## 2. 项目概况

- 根项目名：`AndroidProject`；唯一 Application 模块：`demo`。
- `demo` 的 namespace / applicationId：`com.demo.project`。
- 工程以 Kotlin 为主；`lib_annotation` 和 `lib_processor` 为 Java 注解 / 注解处理器模块。
- 主要能力：网络、WebView、二维码扫描、图片选择、分享、URL Scheme、登录 Hook 与音视频播放。
- Android 配置：`compileSdk = 35`、`minSdk = 24`、`targetSdk = 35`。
- 构建环境：Gradle Wrapper 8.13、AGP 8.13.0、Kotlin 2.2.20、JDK 17。
- 大多数 Android 模块仍以 Java 11 / Kotlin JVM 11 编译；`lib_common-scan` 为 Java/Kotlin 1.8。Gradle 必须运行于 JDK 17。
- 模块列表与仓库配置以 `settings.gradle` 为准；SDK 版本、应用版本及 `local.properties` / 环境变量读取逻辑以 `versions.gradle` 为准。
- 根 `build.gradle` 会应用蒲公英上传与发布脚本。除非需求明确，不要更改签名、发布、上传或依赖仓库行为。

真实配置始终以当前文件为准，不要只依赖本文件中的版本号：

- `settings.gradle`
- `build.gradle`
- `versions.gradle`
- `gradle/wrapper/gradle-wrapper.properties`
- 各模块 `build.gradle`

## 3. 重要模块

- `demo`：唯一应用，组合演示各 common 能力；入口为 `demo/src/main/java/com/demo/project/ProjectApplication.kt`。
- `lib_annotation` / `lib_processor`：登录 APT 方案。前者定义 `@RequireLogin`、`@LoginPage`、`@CheckLogin`；后者使用 AutoService / JavaPoet 在编译期生成登录跳转辅助代码。
- `lib_common-theme`：Application 基类和主题基础能力。
- `lib_common-utils`：权限、缓存、Toast、DataStore、设备、日志、键盘、状态栏、刷新和富文本等通用工具。
- `lib_common-ui`：`BaseActivity`、`BaseFragment`、`BaseViewModel`、ViewBinding 页面容器、页面状态与换肤封装。
- `lib_common-http`：Retrofit / OkHttp、Cookie、缓存、下载、Room 及 JSON 解析；Debug 使用 Chucker，Release 使用 no-op。
- `lib_common-aop`：AspectJ 实现的 `@SingleClick`、`@CheckNet`、`@CheckPermissions` 等横切能力。
- `lib_common-weight`：自定义控件、WebView 容器和带手势的 `VideoPlayerView`。
- `lib_common-share`、`lib-common-media-picker`、`lib_common-scan`：分享、相册/图片选择和二维码扫描能力。
- `lib_common-player`：FFmpeg + AudioTrack / Surface C++ JNI 播放内核，CMake 入口为 `src/main/cpp/CMakeLists.txt`。

`demo` 组合全部 common 模块。公共能力应沉淀到职责匹配的 common 模块，具体演示或业务逻辑留在 `demo`；不得引入反向依赖或循环依赖。核心依赖为 `lib_common-ui → utils/theme/weight/aop`、`lib_common-http → utils/theme`、`lib_processor → lib_annotation`。

## 4. 架构与技术要点

- `demo` 的主要包：`ui/{activity,fragment,dialog,view}`、`vm`、`repository/{local,remote}`、`web/command`、`utils/scheme`、`player/audio`、`constants`。
- 新页面优先复用 `BaseActivity`、`BaseFragment`、`BaseAppVMActivity` 等基类；页面和 ViewModel 状态通信应沿用 `BaseViewModel` 的既有事件通道，不直接耦合 Activity / Fragment。
- 通用 Client、拦截器、缓存和下载逻辑置于 `lib_common-http`，业务 API 定义放入实际使用方模块。
- AOP 保持通用、轻量；不要在切面中写 `demo` 的业务逻辑。
- 新增、删除或调整登录保护时，先检查页面注解、处理器和生成代码链路，避免手写重复登录拦截。

### 播放引擎

- 音频：`AudioPlayerEngine` 为统一接口；`ExoAudioPlayerEngine` 是默认 Media3 实现，`FfmpegAudioPlayerEngine` 使用 FFmpeg；通过 `AudioEngineType` 和 `AudioPlayerEngines` 注册表扩展。
- 视频：`VideoPlayerView` 使用 `VideoPlayerEngine` / `VideoPlayerEngineFactory` 解耦内核；默认 `ExoVideoPlayerEngine`，`demo` 的 `CommonPlayerVideoEngine` 使用 FFmpeg。必须在 `initData()` 或 `setVideoPath()` 前注入引擎。
- `VideoPlayerActivity` 同时支持内部 URL 与外部 `ACTION_VIEW`；修改入口、媒体类型或横竖屏行为时应覆盖这两条路径。
- FFmpeg native 库当前仅支持 `arm64-v8a` 与 `armeabi-v7a`，不应把不匹配 ABI 的模拟器测试当作 native 运行验证。

## 5. 通用编码规则

- 改动保持小范围、贴近需求，优先复用现有基类、工具、组件、扩展函数和模块约定。
- 不做无关重构、重命名、全量格式化、依赖/AGP/Kotlin 升级；不随意新增第三方依赖。
- 不修改生成文件、构建产物或 IDE 临时文件；不通过吞异常、强制非空、硬编码或删除功能来让构建通过。
- 修改公共 API、基类、Application 初始化、全局网络/主题/存储/日志组件、AOP 切面或播放器前，必须先搜索调用方并说明影响范围；优先局部扩展或子类。
- Kotlin / Java 混编时检查空安全、平台类型、默认参数、注解可见性和代码生成兼容性；沿用目标模块的语言和风格。
- Fragment binding 与 View 只允许在视图生命周期内访问；在 `onDestroyView()` 后取消或释放所有依赖 View/Context 的回调、Handler、监听器、Flow、LiveData、协程和播放器。
- Dialog 或 Fragment 事务应检查 Activity、FragmentManager 和状态保存情况；不在主线程做网络、数据库大查询、文件 I/O 或 native 阻塞调用，UI 更新回到主线程。
- 网络应区分成功、业务失败、HTTP 失败、超时、取消和解析异常；文件、Cursor、ResponseBody、流和 native/播放器资源须正确关闭或释放。
- 修改 Room Entity、索引、字段、数据库版本、偏好 key、缓存或序列化模型时考虑 Migration 与历史数据兼容。
- 修改 Manifest、权限、FileProvider、导出组件、WebView、JSBridge、混淆、反射、路由或 JNI 时，检查安全、R8 和跨模块兼容性。全部代码须遵循现有 `minSdk 24` 兼容策略。

## 6. 安全与隐私

不要在回复、日志、新文件、测试代码或提交信息中泄露：

- 签名、证书、密码、私钥、API Secret。
- 仓库账号、访问凭据、token、Cookie、Authorization。
- 真实服务地址、Webhook、完整敏感请求/响应、演示账号或用户隐私数据。

除非任务明确需要，不要读取或输出 `local.properties`、签名配置、私有 Gradle 属性、本机 SSH 配置或账号凭据。需要配置时使用 `versions.gradle` 的 `readLocalOrEnv(...)`、环境变量或占位符；不要把密钥写入源码。

## 7. 开始工作前

无论实施还是审查，先执行或完成等价检查：

```bash
git status --short
git diff --stat
git diff
```

然后：

1. 阅读用户原始需求或指定文档。
2. 阅读涉及文件和完整调用链，搜索相似实现和公共工具。
3. 确认相关模块确实被 `settings.gradle` 引入，并核对目标模块的语言和构建配置。
4. 确认未跟踪文件和用户已有改动；不覆盖、不回退、不清理这些改动。
5. 评估生命周期、线程、网络、文件、数据库、路由、AOP 和 JNI 影响。

## 8. Claude Code 审查流程

审查 Codex 实现时，必须以用户原始需求和实际 Git 变更为依据。

### 8.1 确定审查范围

优先使用自动化脚本提供的基准提交，例如：

```text
BASE_COMMIT=<commit>
```

存在基准提交时执行：

```bash
git status --short
git diff --check "$BASE_COMMIT"
git diff --stat "$BASE_COMMIT"
git diff "$BASE_COMMIT"
```

没有基准提交时，至少执行：

```bash
git status --short
git diff --check
git diff --stat
git diff
```

`git diff` 默认不展示未跟踪文件，必须根据 `git status --short` 找出 `??` 文件并逐个读取。若执行过程中产生提交，则从基准提交到当前 `HEAD` 审查全部提交和工作区变更，不得只看最后一个文件或最后一次 Diff。

### 8.2 需求与 Android 专项检查

逐条检查需求完整性、异常路径和验收标准，确认没有擅自扩大/缩小范围或修改禁止范围。重点检查：

- Activity、Fragment、Application 生命周期，View / Context / binding 泄漏和销毁后回调。
- 主线程 I/O、非主线程 UI 更新、协程/Flow/监听器未取消。
- Java/Kotlin 空安全、Dialog 状态保存、重复点击、定时任务、并发、幂等性和有界重试。
- 文件流、Cursor、ResponseBody、临时文件或 native 资源未释放。
- Room Migration、历史数据、`minSdk 24`、Manifest / 权限 / FileProvider / PendingIntent 安全。
- R8 / ProGuard、反射、序列化、注解生成、路由、WebView / JSBridge、播放器、扫描、分享等跨模块回归。

每个问题必须明确：问题位置、触发条件、实际影响、现有保护为何不足，以及最小修复方向。不要报告个人风格偏好、没有触发路径的猜测、与本次变更无关的历史问题，或当前 Diff 已修复的问题。

## 9. 验证

使用 JDK 17；本机路径不同可用等效路径，但不要写入共享文件。

```bash
# 主应用
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:assembleDebug
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:testDebugUnitTest

# 单模块 / 单个测试
JAVA_HOME=/path/to/jdk-17 ./gradlew :lib_common-utils:testDebugUnitTest
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:testDebugUnitTest --tests "com.demo.project.SomeTest"

# 仪器测试（需要连接匹配 ABI 的设备或模拟器）
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:connectedDebugAndroidTest
```

- 修改 `demo`、资源、Manifest、路由或混淆：至少运行 `:demo:assembleDebug`。
- 修改独立 common 模块：运行相应模块构建或单测；若影响集成，再运行 `:demo:assembleDebug`。
- 修改公共组件、根构建、版本目录、AOP 或注解处理器：优先运行 `:demo:assembleDebug`。
- 修改 FFmpeg/JNI：同时记录构建结果以及未验证的目标 ABI、真机、Surface 或音频焦点场景。
- 只记录实际执行的命令。失败时记录关键错误和退出状态；因环境、网络、JDK、SDK 或私有依赖不能运行时，列为未验证内容。审查模式不得为通过构建而修改源码。

## 10. 审查严重等级

### P0：严重

- 数据不可恢复丢失或破坏。
- 明确安全漏洞或敏感信息泄露。
- 大范围用户稳定崩溃、核心流程完全不可用，或会发布错误/破坏生产构建配置。

### P1：高

- 主要需求未实现或方向错误。
- 常见路径稳定崩溃、明确并发错误/重复提交/状态错乱，或主要功能回归。
- 明显违反接口、数据或兼容性约束，或本次改动导致构建/关键测试失败。

### P2：中

- 边界条件下功能错误、生命周期/资源释放/兼容性隐患、次要流程回归。
- 缺少必要异常处理，或缺少能够阻止高风险回归的关键测试。

不影响正确性、稳定性、安全性或可维护边界的风格建议，不作为正式问题输出。

## 11. 审查结论与输出格式

审查结论只能是：`通过`、`有阻塞问题`、`有非阻塞问题` 或 `无法完成审查`。

- 存在 P0 或 P1：`有阻塞问题`。
- 只有 P2：`有非阻塞问题`。
- 没有发现高置信度问题且已完成必要检查：`通过`。
- 无法读取关键需求、Diff 或项目代码：`无法完成审查`。

审查模式必须使用：

```markdown
# Claude Code 审查报告

## 审查结论

通过 / 有阻塞问题 / 有非阻塞问题 / 无法完成审查

## 问题列表

### P0 严重

没有则写“无”。

### P1 高

没有则写“无”。

### P2 中

没有则写“无”。

每个问题包含：文件路径、代码位置或行号、问题描述、触发条件、实际影响和修复建议。

## 需求覆盖情况

- 逐条对应用户需求或验收标准，标记：已实现 / 部分实现 / 未实现 / 无法验证。

## 变更范围

- 已修改文件、新增未跟踪文件、是否存在无关修改。

## 已执行检查

- 命令、结果、关键失败信息。

## 未验证内容

- 真机、系统版本、网络、服务端、账号、私有依赖和 ABI 等限制。

## 总结

如果没有明确问题，写：“未发现阻塞性问题。”
```

问题按严重程度排序，同一等级按影响范围排序。“通过”不代表所有设备和线上场景均已验证，仍须列出未验证内容。

## 12. Git 与高风险操作

无论哪种模式，都不要主动执行：

```bash
git push
git reset --hard
git clean
rm -rf
```

除非用户明确要求，也不要执行：

```bash
git add
git commit
git reset
git restore
git checkout
```

不覆盖或回退用户已有未提交改动，不改写 Git 历史，不自动切换分支；个人偏好或本机路径写入 `CLAUDE.local.md`，不要写入共享项目记忆。

## 13. 工作完成前复查

### 审查模式

- 已读取原始需求，并确认基准提交或说明无基准。
- 已检查 `git status --short`、完整 Diff 和未跟踪文件。
- 已阅读变更上下文，核对需求符合度和 Android 专项风险。
- 已运行适用检查，或明确未运行原因。
- 未修改任何项目文件；每个问题均有明确触发条件和影响。

### 实现模式

- 已复查最终 Diff，确认没有无关重构、调试代码或敏感信息。
- 已执行适用构建或测试，或如实说明未验证原因。
- 未覆盖用户原有改动，未主动提交或推送。
