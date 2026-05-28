# WebView 通用组件

`com.common.weight.webview` 提供一套可复用的 WebView 基础组件，覆盖：嵌套滑动 WebView、默认 `WebSettings/Client` 配置、H5 ↔ 原生命令桥、浏览历史/书签/收藏的本地存储。该模块是从 `app/src/main/java/com/dian/demo/utils/webview` 抽取而来，做了下文列出的一系列解耦与风险修复。

## 目录结构

```
com/common/weight/webview/
├── bean/
│   ├── JsParam.kt              ── H5 投递给原生的统一参数协议 (name + param)
│   └── WebDataEntry.kt         ── 历史/书签/收藏通用记录 + isSame()
├── callback/
│   ├── WebViewCallBack.kt      ── 页面加载/标题/重定向回调
│   ├── LoadProgressCallBack.kt ── 进度回调 (fun interface)
│   ├── IShareCallBack.kt       ── 分享信息回调 (fun interface)
│   └── IWebMenuListener.kt     ── 浏览器菜单动作
├── command/
│   ├── Command.kt              ── 原生命令接口
│   ├── CommandCallback.kt      ── 命令执行完毕回灌结果给 H5
│   └── CommandBridge.kt        ── 命令转发抽象（取代 AIDL 强耦合）
├── dispatcher/
│   ├── WebCommandDispatcher.kt ── 单例分发入口，可注入自定义 CommandBridge
│   ├── WebCommandRegistry.kt   ── Command 注册中心 (ConcurrentHashMap)
│   └── InProcessCommandBridge.kt ── 默认进程内 Bridge 实现 (Gson 解析)
├── storage/
│   ├── WebPageStore.kt         ── 历史/书签/收藏共用的存储模板 (DataStore + Moshi)
│   ├── WebHistoryUtil.kt       ── 浏览历史 (默认 100 条)
│   ├── WebBookMarkUtil.kt      ── 书签 (无上限)
│   └── CollectWebPageUtil.kt   ── 收藏 (无上限)
├── webset/
│   ├── DefaultWebSetting.kt    ── 默认 WebSettings 配置 (无状态函数)
│   ├── DefaultWebChromeClient.kt
│   └── DefaultWebViewClient.kt
└── webview/
    ├── BaseWebView.kt          ── 嵌套滑动 + JS Bridge 入口
    └── BrowserWebView.kt       ── 浏览器场景的 goTop / getShareData
```

## 模块依赖

`lib_common-weight/build.gradle` 增加了：

```gradle
implementation project(":lib_common-utils")   // LogUtil, AppDataStore, MoshiUtil
implementation project(":lib_common-theme")   // BaseApplication.getAppContext()
implementation 'com.google.code.gson:gson:2.10.1' // JS Bridge 参数解析
```

`minSdk = 24`，所有 `Build.VERSION.SDK_INT < 24` 的判断都已经移除。

## 快速接入

### 1. 在 XML 中使用 `BrowserWebView`

```xml
<com.common.weight.webview.webview.BrowserWebView
    android:id="@+id/webView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### 2. 注册回调并加载

```kotlin
val webView = findViewById<BrowserWebView>(R.id.webView)
webView.initWebClient(object : WebViewCallBack {
    override fun pageStarted(url: String?)  { progressBar.show() }
    override fun pageFinished(url: String?) { progressBar.hide() }
    override fun pageError()                { showError() }
    override fun updateTitle(title: String?) { titleBar.setCenterText(title) }
    override fun overrideUrlLoading(view: WebView?, req: WebResourceRequest?) = false
})
webView.getChromeClient()?.setLoadProgressCallBack { progress -> progressBar.progress = progress }
webView.loadUrl("https://example.com")
```

### 3. 在 `Application` 中注册 H5 → 原生命令

```kotlin
class App : BaseApplication() {
    override fun onCreate() {
        super.onCreate()
        // 在这里注册全部 Command；不再需要 AutoService/ServiceLoader
        WebCommandRegistry.registerAll(listOf(
            ShowToastCommand(this),
            OpenActivityCommand(this)
        ))
    }
}

class ShowToastCommand(private val ctx: Context) : Command {
    override fun name() = "showToast"
    override fun execute(params: Map<String, Any?>?, callback: CommandCallback?) {
        val msg = params?.get("message")?.toString().orEmpty()
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
```

### 4. H5 端调用

```js
window.webview.takeNativeAction(JSON.stringify({
  name: "showToast",
  param: { message: "Hello from H5" }
}));

// 需要回灌结果时
window.webview.takeNativeAction(JSON.stringify({
  name: "login",
  param: { callbackname: "onLoginResult" }
}));

// 原生通过 callback 回灌：demojs.callback("onLoginResult", { token: "xxx" })
```

## 核心类速览

### `BaseWebView`

| 能力 | 说明 |
| --- | --- |
| `NestedScrollingChild` | 与 `CoordinatorLayout` / 自定义嵌套滑动容器联动 |
| `DefaultWebSetting.apply(this)` | 构造时自动应用默认配置 |
| `@JavascriptInterface fun takeNativeAction(json)` | JS 桥入口 |
| `handleCallback(name, response)` | 用 `JSONObject.quote` 转义后回灌 JS（防止换行/引号截断） |
| `initWebClient(WebViewCallBack)` | 注入默认 `WebViewClient` + `WebChromeClient` |
| `clearWebViewData(context)` | 仅清理 WebView 自身数据，不再误删 App `cacheDir`/`externalCacheDir` |

### `BrowserWebView`

| 方法 | 说明 |
| --- | --- |
| `goTop()` | `requestAnimationFrame` 500ms 平滑回顶 |
| `getShareData(IShareCallBack)` | 抽取标题、描述、大于 200×100 的封面图 |

### `WebCommandDispatcher`

```kotlin
WebCommandDispatcher.executeCommand(name, jsonParams, webView)   // 由 BaseWebView 内部调用
WebCommandDispatcher.setBridge(myAidlBridge)                     // 多进程场景注入 AIDL 适配
```

### `WebCommandRegistry`

```kotlin
WebCommandRegistry.register(cmd)
WebCommandRegistry.registerAll(listOf(c1, c2))
WebCommandRegistry.unregister("showToast")
```

### 存储工具

```kotlin
// 历史 (默认最多 100 条；按 timestamp 倒序；按 (title,url) 去重)
WebHistoryUtil.putWebHistory(WebDataEntry("百度", "https://baidu.com", System.currentTimeMillis()))
val histories = WebHistoryUtil.getWebHistoryList()

// 书签 (无上限)
WebBookMarkUtil.markWebPage(entry)
WebBookMarkUtil.isMarkWebPage(entry)
WebBookMarkUtil.removeMarkWebPage(entry)
val marks = WebBookMarkUtil.getMarkWebPage()

// 收藏 (无上限)
CollectWebPageUtil.collectWebPage(entry)
CollectWebPageUtil.removeCollectWebPage(entry)
CollectWebPageUtil.isCollectedWebPage(entry)
val collects = CollectWebPageUtil.getCollectWebPage()
```

底层均委托给 [`AppDataStore`](../../../../../../lib_common-utils/src/main/java/com/common/utils/datastore/AppDataStore.kt) + [`MoshiUtil`](../../../../../../lib_common-utils/src/main/java/com/common/utils/moshi/MoshiUtil.kt)，通过 `WebPageStore` 模板实现。

## 优化清单（相对原 `app/utils/webview`）

| # | 文件 | 问题 | 修复 |
| - | --- | --- | --- |
| 1 | `BaseWebView` | `mChildHelper` 声明为可空但全程 `!!`，构造期任何回调命中即 NPE | 改为 `private val childHelper = NestedScrollingChildHelper(this)`，非空 |
| 2 | `BaseWebView` | `clearWebViewData()` 中 `deleteRecursively()` 了 `cacheDir/codeCacheDir/externalCacheDir` —— 这些是整个 App 共享目录，会误删图片缓存、临时文件 | 仅清理 `app_webview` 私有目录与 `WebViewPrefs` SharedPreferences |
| 3 | `BaseWebView` | `clearWebViewData()` 中 `WebView(context).apply { ... }` 创建了一个临时 WebView 调 `clearCache(true)`，对象未销毁 → 泄漏；且这并不会清理当前 WebView 实例 | 改为对当前 `this` 调 `clearCache/clearHistory/clearFormData` |
| 4 | `BaseWebView` | `handleCallback()` 把 `response` 直接拼到 JS 源码中：`demojs.callback('$name',$response)` —— `name` 含 `'`、`response` 含换行/`</script>` 都会破坏 JS 解析或注入 | `JSONObject.quote(callbackName)` 转义；`response` 为空时给 `null` |
| 5 | `BaseWebView` | 构造期立即 `WebCommandDispatcher.instance.initAidlConnection()`，并强行 `bindService` 到 app 的 AIDL Service | 抽象成 `CommandBridge` 接口，默认进程内 Bridge；多进程由宿主注入 |
| 6 | `BaseWebView` | 3 个独立构造函数，且第三个 `attributeSet: AttributeSet` 不允许 null | `@JvmOverloads constructor(...)`，`defStyleAttr` 默认走 `android.R.attr.webViewStyle` |
| 7 | `BaseWebView` | `addJavascriptInterface(this, "webview")` 后无任何接入开关 | 字面量收敛到 `JS_INTERFACE_NAME`；后续可以加开关 |
| 8 | `BaseWebView` | `TextUtils.isEmpty(jsParam)` + `GsonFactory.getSingletonGson().fromJson(...)` 一旦 JS 投递非法 JSON 直接抛崩溃 | `runCatching { gson.fromJson(...) }` 失败时记录并 return |
| 9 | `DefaultWebSetting` | `lateinit var mWebSettings: WebSettings`：多个 WebView 共享同一全局引用，互相覆盖 | 改为无状态 `fun apply(WebView)`，每个 WebView 各自配置 |
| 10 | `DefaultWebSetting` | `domStorageEnabled = true` 写了两次；`databasePath` 在 API 19+ 已废弃 | 去重；`databasePath` 保留为可选项并加 `@Suppress("DEPRECATION")` |
| 11 | `DefaultWebSetting` | `SDK_INT >= JELLY_BEAN / KITKAT` 等已恒真分支 | 全部移除（minSdk 24） |
| 12 | `DefaultWebChromeClient` | `progressCallBack!!.onCurrentProgress(...)` | `?.onCurrentProgress(...)` |
| 13 | `DefaultWebChromeClient` | `Log.d("QWER", ...)` 调试 tag 误入仓库 | 统一走 `LogUtil.d("WebView", ...)`，并打出 level / source / line |
| 14 | `DefaultWebChromeClient` | 重写 `onJsAlert` 只调用 super，无意义 | 删除 |
| 15 | `DefaultWebViewClient` | 重写 `onScaleChanged` 只调用 super，无意义 | 删除 |
| 16 | `BrowserWebView` | `getShareData()` 的 JS 在 `meta[description]` 缺失时 `getAttribute` 抛错，`evaluateJavascript` 返回 `"null"`，进 JSONObject 解析失败 | JS 改成 `descMeta ? descMeta.getAttribute('content') : ""` 等 null-safe 写法 |
| 17 | `BrowserWebView` | `getTitle()!! / getUrl()!!` 强解包 NPE | `.orEmpty()` |
| 18 | `BrowserWebView` | `mutableListOf<String?>()` 与 `contains` 比对引入 null 值 | 改用 `mutableListOf<String>()`，空串过滤 |
| 19 | `WebCommandDispatcher` | `ServiceConnection.onServiceDisconnected/onBindingDied` 里再调 `initAidlConnection()`，传入 `this` 作为 `ServiceConnection` —— 反复 bind 不解绑，可能堆积连接 | 整体替换为 `CommandBridge` 抽象，AIDL 由宿主自行管理生命周期 |
| 20 | `WebCommandDispatcher` | `MainCommandsManager` 用 `ServiceLoader + @AutoService` 注解收集命令，但 `lib_common-weight` 不引 `auto-service` | 改为显式 `WebCommandRegistry.register(...)`，可读、可调试，免 kapt |
| 21 | `Command` 接口 | `parameters: Map<*, *>?` 双星类型，调用方需要硬转 | 改为 `Map<String, Any?>?` |
| 22 | `CollectWebPageUtil / WebBookMarkUtil / WebHistoryUtil` | 三个文件几乎完全重复的 dedup+sort+save 代码 | 抽取 `WebPageStore` 模板基类，三处实现各自只声明 storageKey 与 limit |
| 23 | `WebHistoryUtil` | 写入时按 `(title,url)` dedup 但读取时也按 timestamp 重排两遍，且只有 `getWebHistoryList` 写了 `take(100)` 限制，`putWebHistory` 中也有 take(100) —— 重复 | `WebPageStore` 内统一一次去重排序；`limit` 通过子类声明 |
| 24 | `CollectWebPageUtil/WebBookMarkUtil` | `isCollectedWebPage`/`isMarkWebPage` 拉全量列表后 `any { ... }`，多读一次 JSON 反序列化 | `WebPageStore.contains()` 仍然走同一份反序列化结果，但代码集中、好优化 |
| 25 | `MainCommandService / MainCommandsManager` | AIDL Service 形态强绑定 app | 没有移植到 lib 模块。多进程场景宿主自行声明 Service，并 `WebCommandDispatcher.setBridge(...)` 注入 AIDL 适配 |
| 26 | `CommandOpenActivity / CommandShowToast / CommandWithResult` | 引用 `HomeActivity / ProjectApplication`，是 app 专属命令 | 没有移植到 lib，README 中给出泛化示例 |

## 多进程 / AIDL 接入

如果业务上 WebView 单独跑在 `:webview` 进程，主进程承载 Command 实现，可以自己写一个 AIDL Bridge：

```kotlin
class AidlCommandBridge(context: Context) : CommandBridge {
    private var remote: WebToMainInterface? = null
    init { /* bindService 到主进程的 AIDL Service */ }
    override fun dispatch(commandName: String, jsonParams: String?, callback: CommandCallback) {
        remote?.handleWebCommand(commandName, jsonParams, object : MainToWebInterface.Stub() {
            override fun onResult(name: String?, response: String?) {
                callback.onResult(name.orEmpty(), response)
            }
        })
    }
}

// Application.onCreate:
WebCommandDispatcher.setBridge(AidlCommandBridge(this))
```

`lib_common-weight` 自身不再依赖任何 AIDL，保持纯 Kotlin。

## 注意事项

1. `BaseWebView` 默认 `addJavascriptInterface(this, "webview")` —— 注入点为 `BaseWebView` 自身，因此 `@JavascriptInterface` 必须显式声明的方法才会暴露给 JS。**不要**在 `BaseWebView` 子类中加非 `@JavascriptInterface` 的同名方法，会被混淆/调用错位。
2. `WebDataEntry` 中 `title` 与 `url` 都可空，但 `WebPageStore.dedupAndSort` 按 `(title,url)` 去重 —— 同时为 null/空时所有记录会被合成一条，请确认调用方传值合法。
3. `WebCommandRegistry` 注册采用 `putIfAbsent`，已存在的同名命令不会被覆盖；如需热更新命令实现，先调用 `unregister(name)` 再 `register(...)`。
4. `clearWebViewData` 不会自动调用 `WebView.destroy()`，请调用方按需自行释放。
5. `DefaultWebSetting` 默认开启 `javaScriptEnabled = true` 与 `setGeolocationEnabled(true)`，仅适用于受信任域；加载第三方页面前请按需收紧。
