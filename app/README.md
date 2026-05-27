# app

`app` 是项目的主业务应用模块，包名来自 `build_versions.applicationId`，当前为 `com.dian.demo`。它承载玩 Android 客户端的完整功能，也保留了一部分正在向 common 模块迁移的业务基础设施。

## 模块定位

- 提供主应用入口、页面导航、登录、文章列表、搜索、收藏、积分、WebView、二维码、图片选择、更新弹窗等业务能力。
- 集成网络请求、Room、DataStore、AOP、分享、Bugly、Walle 渠道读取、AIDL、Proto 等应用级能力。
- 作为历史主工程，仍包含一批与 `lib_common-*` 模块相似的工具、网络、分享、权限和 UI 基类代码。

## 主要目录

- `src/main/java/com/dian/demo/base`: Activity、Fragment、ViewModel 和页面状态基类。
- `src/main/java/com/dian/demo/http`: Retrofit/OkHttp、Cookie、缓存、下载、Moshi/Gson 解析等网络层。
- `src/main/java/com/dian/demo/ui`: 业务页面、弹窗、适配器和自定义 View。
- `src/main/java/com/dian/demo/utils`: 工具类、权限、分享、WebView、二维码、键盘、DataStore、AOP 等。
- `src/main/aidl`: WebView 主进程和子进程通信接口。
- `src/main/proto`: Proto 数据结构。
- `src/main/assets`: WebView 或本地页面资源。

## 关键依赖

- AndroidX、Material、ConstraintLayout、Navigation、Lifecycle、Room。
- Retrofit、OkHttp、Moshi、Gson。
- Glide、XBanner、MagicIndicator、SmartRefreshLayout。
- QQ、微信、微博 SDK。
- AspectJ、Bugly、Walle、DataStore、ZXing、ijkplayer。
- `lib_annotation` + `lib_processor` 用于登录注解处理。

## 注意事项

- Debug/Release 的 `isDebug` 和 `usesCleartextTraffic` 已按构建类型区分。
- 演示登录账号通过 `demo.login.username` 和 `demo.login.password` 配置，不应写死在源码里。
- app 内仍存在可向 common 模块收敛的重复能力，后续优化时可优先迁移网络层、分享、权限和 UI 基类。
