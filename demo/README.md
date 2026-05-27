# demo

`demo` 是公共模块的集成演示应用，包名为 `com.demo.project`。它用于快速验证 `lib_common-*` 模块在真实 Android Application 中的组合效果。

## 模块定位

- 提供轻量级 Demo App，验证主题、UI 基类、网络、工具、AOP、分享和登录注解处理器。
- 作为 common 模块改动后的冒烟入口，适合快速确认模块依赖是否完整、资源是否冲突、基础页面是否可运行。

## 主要内容

- `ProjectApplication`: Demo Application 初始化入口。
- `ui/SplashActivity`: 启动页。
- `ui/HomeActivity`: Demo 主页。
- `vm/MainViewModel`: Demo 页面 ViewModel。

## 依赖关系

`demo` 依赖以下公共模块：

- `lib_common-theme`
- `lib_common-utils`
- `lib_common-weight`
- `lib_common-ui`
- `lib_common-http`
- `lib_common-aop`
- `lib_common-share`
- `lib_annotation`
- `lib_processor`

## 使用建议

- common 模块发生结构性调整后，优先运行 `./gradlew :demo:assembleDebug` 做集成验证。
- Demo 中新增页面时，尽量使用 common 模块提供的基类和工具，避免重新复制 app 内旧实现。
