# CommonTitleBar 通用标题栏

`com.common.weight.titlebar` 下的一组组件，提供可在 XML 直接声明、支持沉浸式状态栏的通用 Android 标题栏，包含左/中/右三区位的多种预设视图（文本、ImageButton、搜索框、自定义布局），并附带状态栏、屏幕度量与键盘冲突修复等工具类。

## 目录文件

| 文件 | 作用 |
| --- | --- |
| `CommonTitleBar.kt` | 标题栏主控件（继承自 `RelativeLayout`） |
| `StatusBarUtils.kt` | 状态栏透明化、亮/暗色图标、状态栏/导航栏高度等工具 |
| `ScreenUtils.kt` | 屏幕尺寸、`dp/px/sp` 互转、软键盘显示/隐藏 |
| `OSUtils.kt` | 厂商 ROM 检测（MIUI / EMUI / Flyme / OPPO / VIVO 等） |
| `FlymeStatusBarUtils.kt` | Flyme 设备状态栏深色图标兼容 |
| `KeyboardConflictCompat.kt` | 修复沉浸式标题栏下软键盘弹起导致的布局错位 |

## 功能特性

- 沉浸式状态栏：自动撑起状态栏区域，可单独设置颜色、亮/暗色图标。
- 左/中/右三区位独立配置，每个区位都支持 `none / textView / imageButton(or searchView) / customView` 四种类型。
- 中间区位支持：纯标题（带副标题与跑马灯）、搜索框（语音/删除切换）、完全自定义视图。
- 底部分割线与底部阴影二选一。
- 支持单击与双击两种监听。
- 运行时可动态切换状态栏明暗模式、替换左右按钮图标/文字、显示/隐藏中间加载进度条。

## 快速接入

### 1. XML 中声明

```xml
<com.common.weight.titlebar.CommonTitleBar
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:titleBarColor="@color/white"
    app:titleBarHeight="44dp"
    app:statusBarColor="@color/white"
    app:statusBarMode="dark"
    app:showBottomLine="true"
    app:bottomLineColor="#DDDDDD"
    app:leftType="imageButton"
    app:leftImageResource="@drawable/comm_titlebar_reback_selector"
    app:centerType="textView"
    app:centerText="标题"
    app:centerTextColor="#333333"
    app:centerTextSize="16sp"
    app:rightType="textView"
    app:rightText="完成"
    app:rightTextColor="@color/comm_titlebar_text_selector"
    app:rightTextSize="15sp" />
```

### 2. 代码中监听

```kotlin
titleBar.setListener { v, action, extra ->
    when (action) {
        CommonTitleBar.ACTION_LEFT_BUTTON   -> finish()
        CommonTitleBar.ACTION_RIGHT_TEXT    -> submit()
        CommonTitleBar.ACTION_SEARCH_SUBMIT -> doSearch(extra.orEmpty())
        CommonTitleBar.ACTION_SEARCH_VOICE  -> startVoiceInput()
    }
}

titleBar.setDoubleClickListener { scrollToTop() }
```

## XML 属性一览（`R.styleable.CommonTitleBar`）

### 状态栏 / 整体

| 属性 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `openStatusBar` | boolean | true | 是否启用沉浸式状态栏 |
| `fillStatusBar` | boolean | true | 是否撑起状态栏区域 |
| `statusBarColor` | color | `#FFFFFF` | 状态栏填充区颜色 |
| `statusBarMode` | enum(`dark`/`light`) | dark | 状态栏图标主题 |
| `titleBarColor` | color | `#FFFFFF` | 标题栏背景色 |
| `titleBarHeight` | dimension | 44dp | 标题栏高度 |
| `showBottomLine` | boolean | true | 是否显示底部分隔线 |
| `bottomLineColor` | color | `#DDDDDD` | 底部分隔线颜色 |
| `bottomShadowHeight` | dimension | 0 | 底部阴影高度（`showBottomLine = false` 时生效） |

### 左侧区位 `leftType`

| 取值 | 含义 | 关联属性 |
| --- | --- | --- |
| `none(0)` | 不显示 | — |
| `textView(1)` | 文本 | `leftText` `leftTextColor` `leftTextSize` `leftDrawable` `leftDrawablePadding` |
| `imageButton(2)` | 图标按钮 | `leftImageResource` |
| `customView(3)` | 自定义布局 | `leftCustomView` |

### 右侧区位 `rightType`

| 取值 | 含义 | 关联属性 |
| --- | --- | --- |
| `none(0)` | 不显示 | — |
| `textView(1)` | 文本 | `rightText` `rightTextColor` `rightTextSize` |
| `imageButton(2)` | 图标按钮 | `rightImageResource` |
| `customView(3)` | 自定义布局 | `rightCustomView` |

### 中间区位 `centerType`

| 取值 | 含义 | 关联属性 |
| --- | --- | --- |
| `none(0)` | 不显示 | — |
| `textView(1)` | 主/副标题 | `centerText` `centerTextColor` `centerTextSize` `centerTextMarquee` `centerSubText` `centerSubTextColor` `centerSubTextSize` |
| `searchView(2)` | 搜索框 | `centerSearchEditable` `centerSearchBg` `centerSearchRightType`(`voice`/`delete`) |
| `customView(3)` | 自定义布局 | `centerCustomView` |

## 事件 Action 常量

回调中通过 `action` 区分点击源：

| 常量 | 触发时机 |
| --- | --- |
| `ACTION_LEFT_TEXT` | 左侧 TextView 点击 |
| `ACTION_LEFT_BUTTON` | 左侧 ImageButton 点击 |
| `ACTION_RIGHT_TEXT` | 右侧 TextView 点击 |
| `ACTION_RIGHT_BUTTON` | 右侧 ImageButton 点击 |
| `ACTION_SEARCH` | 搜索框被点击（不可输入态触发，可输入态仅显示光标） |
| `ACTION_SEARCH_SUBMIT` | 输入态下按下键盘搜索键，`extra` 为输入内容 |
| `ACTION_SEARCH_VOICE` | 语音按钮被点击 |
| `ACTION_SEARCH_DELETE` | 搜索框右侧的清空按钮被点击 |
| `ACTION_CENTER_TEXT` | 中间标题文本被点击 |

## 常用运行时 API

```kotlin
titleBar.setCenterText("新标题")
titleBar.setLeftIcon(R.drawable.ic_back)
titleBar.setRightIcon(R.drawable.ic_more)
titleBar.setRightText("发布", Color.BLACK, 15)
titleBar.setLeftVisibility(View.GONE)

titleBar.setStatusBarColor(Color.WHITE)
titleBar.showStatusBar(true)
titleBar.toggleStatusBarMode() // 切换状态栏图标明/暗

titleBar.showCenterProgress()
titleBar.dismissCenterProgress()

titleBar.showSoftInputKeyboard(true)  // searchView 模式下显示键盘
val keyword: String = titleBar.getSearchKey()
```

获取内部视图（按 `Type` 返回对应控件，未启用对应类型时为 `null`）：

```kotlin
titleBar.getLeftTextView()        // leftType = textView
titleBar.getLeftImageButton()     // leftType = imageButton
titleBar.getLeftCustomView()      // leftType = customView
titleBar.getCenterTextView()      // centerType = textView
titleBar.getCenterSubTextView()
titleBar.getCenterSearchEditText()
titleBar.getCenterSearchLeftImageView()
titleBar.getCenterSearchRightImageView()
titleBar.getCenterCustomView()
titleBar.getRightTextView()
titleBar.getRightImageButton()
titleBar.getRightCustomView()
titleBar.getMainView()            // 标题栏内部主容器
titleBar.getBottomLine()          // 底部分割线 View
```

也可在初始化后用代码注入任意区位的自定义视图，框架会先移除该区位已存在的视图：

```kotlin
titleBar.setLeftView(myLeftView)
titleBar.setCenterView(myCenterView)
titleBar.setRightView(myRightView)
```

## 配套工具类

### StatusBarUtils

```kotlin
StatusBarUtils.transparentStatusBar(window)
StatusBarUtils.setDarkMode(window)     // 暗色图标
StatusBarUtils.setLightMode(window)    // 浅色图标
StatusBarUtils.setStatusBarColor(window, color, alpha)
val h = StatusBarUtils.getStatusBarHeight(context)
val navH = StatusBarUtils.getNavigationBarHeight(context)
val hasNav = StatusBarUtils.checkDeviceHasNavigationBar(context)
```

### ScreenUtils

```kotlin
ScreenUtils.dp2PxInt(context, 12f)
ScreenUtils.sp2px(context, 14f)
val size: IntArray = ScreenUtils.getScreenPixelSize(context) // [w, h]
ScreenUtils.showSoftInputKeyBoard(context, editText)
ScreenUtils.hideSoftInputKeyBoard(context, editText)
```

### OSUtils

```kotlin
OSUtils.isMiui()    OSUtils.isEmui()    OSUtils.isFlyme()
OSUtils.isOppo()    OSUtils.isVivo()    OSUtils.isSmartisan()
OSUtils.getName()   OSUtils.getVersion()
```

### KeyboardConflictCompat

沉浸式标题栏下软键盘弹起会出现布局被裁切问题，在 `Activity.onCreate` 中调用：

```kotlin
KeyboardConflictCompat.assistWindow(window)
```

内部会在 `contentView` detach 时自动反注册 `OnGlobalLayoutListener`，避免泄漏。

## 注意事项

- `minSdk` 要求 ≥ 24（本工程 24）。
- 该控件 `layout_height` 建议使用 `wrap_content`，控件内部会按 `titleBarHeight + 状态栏高度` 自行计算。
- `centerType = searchView` 模式下，若同时设置了左/右按钮，搜索框的水平外边距会根据按钮位置自动收缩；若希望搜索框紧贴标题栏左右边缘，请将对应方向的 `leftType / rightType` 设为 `none`。
- 默认会启用沉浸式状态栏（`openStatusBar = true`），如果 `Activity` 不希望被改变状态栏样式，请在 XML 中显式声明 `app:openStatusBar="false"`。
