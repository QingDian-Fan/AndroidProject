# AGENTS.md

本文件用于指导 Codex 在 AndroidProject 中分析、实现、审查和验证需求。

## 1. Codex 工作模式与目标

Codex 在本项目中有实现模式和审查模式，当前角色由用户要求或自动化脚本提示词决定。不得在同一次自动化任务中擅自切换角色。

### 1.1 实现模式

当用户明确要求 Codex 编写、修改或修复代码，或由 `.scripts/ai-codex-task.sh` 启动时，进入实现模式。主要职责是：

1. 准确理解用户下发的需求。
2. 在既有架构、模块边界和编码风格内完成最小必要改动。
3. 对改动执行适当的编译、测试和静态自检。
4. 保留清晰、未提交的 Git Diff，供独立审查使用。

不要自行扩大需求，不要为了“顺手优化”修改无关代码，不要将自我评价当作独立代码审查结论。

### 1.2 审查模式

当用户要求“代码审查”“review”“审查 Claude”，指定基准提交或要求只读检查当前工作区，或由 `.scripts/ai-claude-task.sh` 启动时，进入审查模式。

审查模式下：

- 只读取、搜索和分析，不得修改、创建、移动、格式化或删除任何项目文件，也不得生成或应用补丁。
- 不得自动修复发现的问题，不得执行会改变 Git 状态的命令。
- 必须以用户原始需求、基准提交和实际 Git Diff 为依据独立审查，不能相信或复述 Claude 的自我评价。
- 必须检查未跟踪文件、完整 Diff、变更上下文和调用链，只报告具有明确触发条件与实际影响的高置信度问题。
- 自动化脚本提供只读 sandbox 时，不得尝试绕过；因此无法执行会写入构建目录的 Gradle 命令时，应将其列入“未验证内容”。

模式不明确时，优先保持只读并先分析需求、Git 状态和相关代码。

## 2. 项目概况

- 根项目名：`AndroidProject`
- 主应用模块：`demo`
- 主包名 / namespace：`com.demo.project`
- 工程类型：以 Kotlin 为主的多模块 Android 示例工程，少量注解处理器为 Java。
- `demo` 是唯一 Application，负责组合和演示各 common library 的能力。
- 当前 Android 配置：`compileSdk = 35`、`minSdk = 24`、`targetSdk = 35`。
- 构建环境：Gradle Wrapper 8.13、AGP 8.13.0、Kotlin 2.2.20、JDK 17。
- 大部分 Android 模块的 `sourceCompatibility` 与 Kotlin `jvmTarget` 为 11；`lib_common-scan` 仍为 1.8。Gradle 守护进程必须使用 JDK 17。
- 版本、SDK 和签名/本地配置读取逻辑集中在根目录 `versions.gradle`；仓库和模块清单以 `settings.gradle` 为准。
- 根构建还应用了 `pgyer-upload.gradle` 与 `publish-app.gradle`；除非需求明确，不要改动上传、发布或签名流程。

真实配置始终以当前仓库文件为准，尤其是：

- `settings.gradle`
- `build.gradle`
- `versions.gradle`
- `gradle/wrapper/gradle-wrapper.properties`
- 各模块的 `build.gradle`

## 3. 模块与依赖边界

- `demo`：唯一应用模块，演示网络、WebView、二维码扫描、图片选择、分享、URL Scheme、音视频播放及登录 Hook 等能力。
- `lib_annotation`：登录流程 APT 的 Java 注解定义，包含 `@RequireLogin`、`@LoginPage`、`@CheckLogin`。
- `lib_processor`：基于 AutoService / JavaPoet 的注解处理器，生成集中式登录跳转辅助代码；依赖 `lib_annotation`。
- `lib_common-theme`：全局 Application 基类和主题基础能力。
- `lib_common-utils`：通用 Kotlin 工具，包括权限、缓存、Toast、DataStore、设备、键盘、日志、状态栏、刷新和富文本等。
- `lib_common-ui`：Activity、Fragment、ViewModel 基类，ViewBinding 页面容器、页面状态和换肤基础封装。
- `lib_common-http`：Retrofit / OkHttp、Cookie、缓存拦截器、下载、Room 及 JSON 解析封装。
- `lib_common-aop`：AspectJ 横切能力，包括防重复点击、网络检查和权限检查。
- `lib_common-weight`：自定义控件，包含 WebView 容器和可注入播放引擎的 `VideoPlayerView`。
- `lib_common-share`：QQ、微信、微博和自定义渠道分享。
- `lib-common-media-picker`：相册浏览、图片预览和多选能力。
- `lib_common-scan`：相机预览与微信 QRCode / OpenCV 二维码扫描能力。
- `lib_common-player`：FFmpeg + AudioTrack / Surface 的 C++ JNI 音视频播放内核。

核心依赖关系是：`demo` 组合所有 common 模块；`lib_common-ui` 依赖 utils/theme/weight/aop，`lib_common-http` 依赖 utils/theme，`lib_processor` 依赖 `lib_annotation`。新增通用能力时，优先放入职责匹配的 common 模块；仅 `demo` 使用的演示或业务代码留在 `demo`，避免反向依赖或循环依赖。

## 4. 架构要点

- Application 入口：`demo/src/main/java/com/demo/project/ProjectApplication.kt`。
- `demo` 主要包结构：`ui/{activity,fragment,dialog,view}`、`vm`、`repository/{local,remote}`、`web/command`、`utils/scheme`、`player/audio`、`constants`。
- 新页面优先复用 `lib_common-ui` 的 `BaseActivity`、`BaseFragment`、`BaseAppVMActivity` 等基类。页面和 ViewModel 的状态、加载、空态、错误、导航和返回应沿用 `BaseViewModel` 的既有事件通道，避免 Activity / Fragment 直接互相耦合。
- 通用网络 Client、拦截器、缓存和下载机制留在 `lib_common-http`；具体业务 API 放在实际使用模块。Debug 的 Chucker 与 Release 的 no-op 行为应保持一致。
- `lib_common-aop` 的 `@SingleClick`、`@CheckNet`、`@CheckPermissions` 通过切面实现横切逻辑。切面只处理通用职责，不放入复杂业务判断。
- 登录 Hook 由 `lib_annotation` + `lib_processor` 在编译期生成。增删需要登录的页面时，先确认注解和生成链路，通常只需调整页面注解，不要手写重复的登录拦截判断。
- `lib_common-player` 的 native 源码位于 `src/main/cpp`，CMake 入口为 `src/main/cpp/CMakeLists.txt`，预编译 so 位于 `src/main/jniLibs`。修改 JNI、C++ 或 native 库加载逻辑时必须检查 Java/Kotlin 签名、CMake 和 ABI 配置。

### 播放引擎边界

- 音频：`AudioPlayerEngine` 是统一接口；`ExoAudioPlayerEngine` 为默认 Media3 实现，`FfmpegAudioPlayerEngine` 使用 FFmpeg 内核；通过 `AudioPlayerEngines` 注册表和 `AudioEngineType` 扩展。
- 视频：`VideoPlayerView` 只依赖 `VideoPlayerEngine` / `VideoPlayerEngineFactory`；默认 `ExoVideoPlayerEngine`，`demo` 的 `CommonPlayerVideoEngine` 使用 FFmpeg。注入引擎必须早于 `initData()` 或 `setVideoPath()`。
- `VideoPlayerActivity` 同时支持外部 `ACTION_VIEW` 和内部 URL；修改其媒体类型或方向逻辑时，应覆盖两种入口。
- FFmpeg 内核当前仅提供 `arm64-v8a`、`armeabi-v7a` ABI。涉及此模块的运行验证必须使用相符 ABI 的真机或模拟器。

## 5. 需求来源与优先级

开始任务前，按以下优先级理解需求：

1. 用户当前输入的明确要求。
2. 用户指定的需求文档、接口文档、设计稿或附件。
3. 当前目录及父级目录中的 `AGENTS.md`。
4. 现有架构、调用链、模块边界和编码风格。
5. 本文件中的通用规则。

发生冲突时，不得擅自忽略用户当前明确要求。小范围歧义应先阅读调用链、相似实现和测试后采取保守实现；无法安全判断的关键行为必须说明风险，不得伪装为已完成，也不要借机重构整条链路。

## 6. 开始任务前的强制检查

每次实现或审查前必须执行或完成等价检查：

```bash
git status --short
git diff --stat
git diff
```

同时完成：

1. 阅读需求直接涉及的文件及其调用入口。
2. 搜索已有相似实现、工具类、扩展函数和注解。
3. 确认真正参与构建的模块和目标模块的语言、SDK/JVM 配置。
4. 确认是否存在用户未提交或未跟踪的改动。
5. 判断需求可能影响的生命周期、线程、网络、文件、数据库、路由、JNI 或注解生成链路。

若工作区存在用户已有修改：

- 不得覆盖、回退、格式化或混入这些改动。
- 不得执行 `git reset`、`git restore`、`git checkout --` 或 `git clean` 清理工作区。
- 修改同一文件前必须先读清楚已有 Diff，并避免改写其中不属于本任务的行。
- 最终应区分本任务改动与发现的既有改动。

## 7. 实现规则

本节适用于实现模式。审查模式只能据此判断实现是否正确，不得直接修改代码。

### 7.1 通用规则

- 改动保持小范围且贴近需求，优先复用现有基类、组件、工具和业务约定。
- 不做无关重构、重命名、全量格式化、依赖升级或构建系统升级。
- 不随意新增第三方依赖，不修改生成文件、构建产物或 IDE 临时文件。
- 不改变既有接口、数据结构或业务语义，除非需求明确要求；修改公共 API 前必须搜索全部调用方。
- 删除代码前必须确认没有 XML、Manifest、反射、注解生成、JNI 或跨模块调用。
- 不通过硬编码、吞异常、无效注释或删除现有功能来让构建通过。

### 7.2 Kotlin / Java 与 Android

- 优先沿用目标模块已有语言和风格，不强行把 Java 改写为 Kotlin。
- Kotlin 代码正确处理可空类型、协程取消、作用域与线程切换；不要用 `!!` 掩盖不确定的空值。
- Java / Kotlin 互调时检查平台类型、空安全、默认参数和注解处理器可见性。
- Activity / Fragment 沿用既有初始化分层；ViewBinding、View 和依赖其 Context 的回调只能在正确生命周期内访问。
- `onDestroyView()` 后不得继续引用 Fragment binding 或 View；Handler、监听器、Flow、LiveData、协程、播放器和回调须随正确生命周期取消或释放。
- Dialog / Fragment 事务应处理 Activity 有效性和 `FragmentManager.isStateSaved`；优先复用项目已有安全展示方式。
- 不在主线程执行网络、数据库大查询、明显文件 I/O 或 native 阻塞调用；UI 更新必须回到主线程。
- 修改 XML、资源、Manifest 或系统栏时，遵循既有深色模式、横竖屏、不同屏幕尺寸和 `minSdk 24` 兼容策略。

### 7.3 网络、文件、数据与并发

- 网络请求应区分成功、业务失败、HTTP 失败、超时、取消和解析异常；不得输出敏感请求头、完整敏感响应或账号信息。
- 文件、Cursor、ResponseBody、流和播放器资源须自动关闭或在适当时机释放。不得上传仍在写入的文件，也不要不安全地移动、截断或长时间锁住原文件。
- 定时任务、重复点击、下载、播放器回调和并发请求要考虑唯一性、幂等性、取消与有界重试，避免重复提交和无限循环。
- 改动 Room Entity、字段、索引或版本时，检查 Migration、历史数据兼容和 schema 输出；变更 SharedPreferences key、序列化模型或缓存结构时同样要考虑存量数据。

### 7.4 公共组件、AOP 与 native 代码

- `BaseActivity`、`BaseFragment`、`BaseViewModel`、Application 初始化、全局网络拦截器、主题/存储/日志组件、通用播放器和 AOP 切面均为高影响范围。确需修改时先搜索全部使用方，优先局部扩展或子类，并在最终结果说明影响面和回归风险。
- 新增 AOP 行为时，确保注解保留策略、切点和目标方法签名正确；不要让切面依赖 `demo` 业务实现。
- 修改播放器状态机、音频焦点、Surface、JNI 或 C++ 时，检查生命周期释放、线程边界、错误回调、ABI 与 native 库加载；不要只在 x86 模拟器上声称 FFmpeg 验证通过。

## 8. 安全与保密

不得在回复、日志、新文件、测试代码或提交信息中泄露：

- 签名文件、别名、密码、证书、私钥或 API Secret。
- 仓库账号、访问凭据、token、Cookie、Authorization。
- 真实服务地址、Webhook、完整请求/响应、演示账号或用户隐私数据。

除非任务明确需要，不要读取或输出与任务无关的 `local.properties`、签名配置、私有 Gradle 属性、本机 SSH 配置或账号凭据文件。配置值应使用 `versions.gradle` 的 `readLocalOrEnv(...)` 注入机制、环境变量或占位符，`local.properties` 不入库。

## 9. Git 与高风险操作

除非用户明确要求，否则不得执行：

```bash
git push
git commit
git add
git reset --hard
git reset
git clean
git checkout --
git restore
rm -rf
```

同时，不创建或切换分支，不改写 Git 历史，不删除用户文件，不将审查报告写入源码目录。完成实现后保留未提交 Diff，供后续独立审查。

## 10. 验证策略

根据改动范围选择最小但足够的验证；构建时使用 JDK 17。若本机 JDK 位置不同，使用本机等效 JDK 17 路径，不要把个人路径写入共享源码。

```bash
# 基础检查
git diff --check
git status --short
git diff --stat
git diff

# 主应用
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:assembleDebug
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:testDebugUnitTest

# 单模块或单测
JAVA_HOME=/path/to/jdk-17 ./gradlew :lib_common-utils:testDebugUnitTest
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:testDebugUnitTest --tests "com.demo.project.SomeTest"

# 仪器测试（须连接匹配设备/模拟器）
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:connectedDebugAndroidTest
```

- 修改 `demo` 源码、资源、Manifest、路由或混淆配置：至少运行 `:demo:assembleDebug`。
- 修改独立 common 模块：至少运行该模块的 `assembleDebug` 或适合的单元测试；若其变更影响 `demo`，再运行 `:demo:assembleDebug`。
- 修改公共组件、根构建、版本、AOP 或注解处理器：优先运行 `:demo:assembleDebug`，以验证 weave / 代码生成和集成。
- 修改 JNI / FFmpeg：运行相应构建，并如实记录未验证的目标 ABI、真机、Surface 或音频焦点场景。
- 无法执行时，记录实际命令、关键失败原因和未验证风险；不得声称未实际执行的检查已经通过。

## 11. Codex 实现完成条件

只有同时满足以下条件，任务才可标记为完成：

1. 已阅读需求、相关文件与调用链。
2. 已实现主要路径和必要异常路径。
3. 未主动覆盖用户已有修改。
4. 已执行 `git diff --check` 并复查最终 Diff。
5. 已运行适用构建或测试，或明确说明无法运行的原因。
6. Diff 中没有明显无关改动、调试代码或敏感信息。
7. 已保留未提交变更供独立审查，未执行提交或推送。

## 12. 自动化协作与独立审查边界

项目提供两条本地自动化协作链路：

- `.scripts/ai-codex-task.sh`：Codex 实现，Claude Code 只读审查。
- `.scripts/ai-claude-task.sh`：Claude Code 实现，Codex 在只读 sandbox 中审查。

两条链路都使用 `.scripts/README.md` 作为需求正文，使用 `.scripts/images/` 提供参考图片，并将需求快照、图片、变更清单、实现日志和审查报告归档到 `.ai-code-reviews/<时间戳>/`。`.scripts/` 与 `.ai-code-reviews/` 是本地自动化输入和产物目录，不应混入业务代码 Diff。

自动化任务使用不同的固定会话文件隔离角色记忆：Codex 实现、Claude 审查、Claude 实现、Codex 审查四个会话不得相互复用。执行期间不得修改需求输入、自动化脚本、`AGENTS.md` 或 `CLAUDE.md`，除非当前用户任务明确就是维护这些文件。

无论哪一方负责实现，都不得把自我评价当作审查结论；无论哪一方负责审查，都不得修改实现。独立审查应以原始需求、基准提交、当前 Git 状态、完整 Diff、未跟踪文件和实际构建/测试结果为依据。

为保证审查准确：不自动提交、不隐藏或清理 Diff、不混入无关格式化、不批量生成无关文件；最终输出应如实记录失败、跳过和未验证项。

## 13. 最终输出格式

完成后按以下结构输出：

```markdown
## 实现结果

- 状态：已完成 / 部分完成 / 未完成
- 需求摘要：...

## 修改文件

- `路径`：修改内容与原因

## 实现说明

- 核心实现方式
- 关键调用链
- 兼容性与异常处理

## 验证结果

- `执行的命令`：通过 / 失败 / 未执行
- 失败原因或环境限制

## 未解决问题与风险

- 没有则写“无已知未解决问题”

## 独立审查提示

- 建议重点检查的高风险位置
- 未验证的设备、场景或接口
```

不得声称未实际执行的命令已经通过。

审查模式使用以下结构：

```markdown
# Codex 代码审查报告

## 审查结论

通过 / 有阻塞问题 / 有非阻塞问题 / 无法完成审查

## 问题列表

按 P0、P1、P2 输出；没有则写“无”。每个问题必须包含文件、代码位置、问题描述、触发条件、实际影响和最小修复建议。

## 需求覆盖情况

- 逐条标记：已实现 / 部分实现 / 未实现 / 无法验证。

## 已执行检查

- 只列实际执行的命令与结果。

## 未验证内容

- 设备、ABI、网络、账号、依赖、权限或只读 sandbox 限制。
```
