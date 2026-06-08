# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

多模块 Android 示例工程。唯一的应用模块 `demo`（namespace `com.demo.project`，applicationId `com.demo.project`）组合并演示一组可复用的 `lib_common-*` 基础库。代码以 Kotlin 为主，UI 层使用 ViewBinding。

- AGP 8.13.0 / Kotlin 2.2.20 / compileSdk 35 / targetSdk 35 / minSdk 24
- 构建需要 **JDK 17**（注意：各 `lib_common-*` 模块的 `compileOptions` 仍声明 Java 11 / jvmTarget 11，但 Gradle 必须跑在 JDK 17 上）

## 常用命令

构建/测试时显式指定 JDK 17（CI 与本机一致）：

```bash
export JAVA_HOME=/Users/dian/Library/Java/JavaVirtualMachines/corretto-17.0.16/Contents/Home

# 编译 Debug APK
./gradlew :demo:assembleDebug

# 单元测试（JVM）
./gradlew :demo:testDebugUnitTest
./gradlew :lib_common-utils:testDebugUnitTest          # 单模块
./gradlew :demo:testDebugUnitTest --tests "com.demo.project.SomeTest"   # 单个测试类

# 仪器测试（需连接设备/模拟器）
./gradlew :demo:connectedDebugAndroidTest

# 修改 AspectJ 切面或注解处理器后，用 assemble 验证 weave / 代码生成是否正常
./gradlew :demo:assembleDebug
```

> FFmpeg native 内核（`lib_common-player`）的 `abiFilters` 仅含 `arm64-v8a` / `armeabi-v7a`，必须在对应 ABI 的真机或模拟器上运行。

## 架构

### 模块分层与依赖

`demo` 依赖全部 common 库；common 库之间的依赖（见根 README 的 mermaid 图）核心是：`lib_common-ui → utils/theme/weight/aop`，`lib_common-http → utils/theme`，`lib_processor → lib_annotation`。**新业务优先沉淀到对应 common 模块，再由 `demo` 组合使用。**

- `lib_common-ui` — Activity/Fragment/ViewModel 基类与 MVVM 约定。新页面应继承 `BaseActivity`/`BaseFragment`/`BaseAppVMActivity` 等；ViewModel 与页面通信走 `BaseViewModel` 的事件通道（加载/空/错误/导航/返回态），不要在 Activity/Fragment 之间直接耦合。换肤见 `skin/*`。
- `lib_common-http` — 统一 Retrofit/OkHttp Client、拦截器、Cookie、缓存、下载（Room）。**业务接口定义放在使用方模块，通用 Client/拦截器留在本模块。** Debug 集成 Chucker，Release 用 no-op。
- `lib_common-aop` — 基于 AspectJ 的横切能力，靠注解触发：`@SingleClick`（防重复点击）、`@CheckNet`、`@CheckPermissions`，对应 `*Aspect` 切面。切面保持轻量，不写复杂业务。
- `lib_common-weight` — 自定义控件，含可注入播放引擎、带手势的 `VideoPlayerView`。
- `lib_common-player` — FFmpeg + AudioTrack/Surface 的 C++/JNI 音视频内核，对外暴露 `FfmpegAudioPlayer` / `FfmpegVideoPlayer`。native 源码在 `src/main/cpp`，预编 so 在 `src/main/jniLibs`，CMake 入口 `src/main/cpp/CMakeLists.txt`。
- `lib_annotation` + `lib_processor` — 登录 Hook 的 APT 方案（AndLogin）。处理器编译期扫描 `@RequireLogin` / `@LoginPage` / `@CheckLogin`，用 JavaPoet 生成集中式登录跳转代码（Hook AMS，未登录先跳登录页、成功后回到原目标页）。**增删需登录的页面只加/删注解即可，无需改判断逻辑。**

### 播放引擎抽象（工厂模式，UI 与内核解耦）

音视频播放是本项目的核心设计点。UI 层只依赖统一接口，底层引擎可切换/拓展：

- 音频：`AudioPlayerEngine`（接口）→ `ExoAudioPlayerEngine`（默认，Media3 ExoPlayer）/ `FfmpegAudioPlayerEngine`（FFmpeg 内核）；工厂注册表 `AudioPlayerEngines`，默认 `EXO_PLAYER`。新增内核：实现 `AudioPlayerEngine` → 在 `AudioEngineType` 加类型 → `AudioPlayerEngines.register()` 注册，UI 无需改动。
- 视频：`VideoPlayerView`（`lib_common-weight`）通过 `setPlayerEngineFactory()` / `setPlayerEngine()` 注入 `VideoPlayerEngine`；默认 `ExoVideoPlayerEngine`，`demo` 提供 FFmpeg 实现 `CommonPlayerVideoEngine`。注入须在 `initData()` / `setVideoPath()` 之前。
- `demo` 的 `VideoPlayerActivity` 同时承接外部 `ACTION_VIEW` 与内部 URL，并按媒体类型自动决定横竖屏。
- 相关代码：`demo/src/main/java/com/demo/project/player/`。

### demo 模块布局

`com.demo.project` 下：`ui/{activity,fragment,dialog,view}`、`vm`、`repository/{local,remote}`、`web/command`、`utils/scheme`、`player/audio`、`constants`。应用类为 `ProjectApplication`。

## 配置与约定

- 敏感配置（签名、蒲公英、乐固、webhook、演示登录账号）通过 `local.properties` 或环境变量读取，统一经 `versions.gradle` 的 `readLocalOrEnv(localKey, ENV_KEY, default)`；`local.properties` 不入库，**不要把密钥写进源码**。
- 版本号/SDK 版本集中在 `versions.gradle` 的 `build_versions`。根 `build.gradle` 还 apply 了 `pgyer-upload.gradle`（蒲公英上传）和 `publish-app.gradle`。
- 仓库与 BOM 在 `settings.gradle`，含 jitpack、google、mavenCentral、aliyun jcenter 镜像。
- 新增代码优先 Kotlin + ViewBinding；新增/改动模块后同步更新根 README 和对应模块 README。
