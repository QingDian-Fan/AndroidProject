# 视频播放器组件（video）

基于 [IjkPlayer](https://github.com/bilibili/ijkplayer) 封装的一套自定义视频播放控件，提供完整的播放控制面板、手势交互（亮度 / 音量 / 进度）以及一个手绘的播放 / 暂停按钮动画。

## 目录结构

| 文件 | 说明 |
| --- | --- |
| `VideoPlayerView.kt` | 播放器主控件，封装 IjkPlayer，承载 UI、手势、控制面板等全部逻辑 |
| `VideoPlayButton.kt` | 自定义播放 / 暂停按钮，使用 `Path` + `PathMeasure` 绘制可变形的过渡动画 |
| `VideoScaleType.kt` | 视频画面缩放模式枚举 |

---

## VideoPlayerView

继承自 `FrameLayout`，内部通过 `view_video_player` 布局填充 `SurfaceView` 及各控制控件。

### 主要特性

- 基于 `IjkMediaPlayer` 的播放、暂停、续播、进度跳转
- 顶部 / 底部控制面板的显示与隐藏动画（平移 + 透明度）
- 锁屏功能（锁定后隐藏控制面板、屏蔽手势）
- 手势交互：
  - **水平滑动** → 快进 / 快退
  - **屏幕左侧垂直滑动** → 调节屏幕亮度
  - **屏幕右侧垂直滑动** → 调节系统音量
- 缓冲进度（二级进度条）与播放进度实时刷新
- 三种画面缩放模式

### 公开 API

| 方法 | 说明 |
| --- | --- |
| `initData()` | 初始化 `SurfaceView` 回调，**使用前必须调用** |
| `setVideoPath(urlString: String)` | 设置视频地址（本地路径或网络 URL） |
| `setTitle(title: String)` | 设置标题栏文字 |
| `setScaleType(scaleType: VideoScaleType)` | 设置画面缩放模式 |
| `setSpeed(speed: Float)` | 设置播放倍速 |
| `setWindow(window: Window)` | 设置宿主窗口，用于亮度调节（默认自动从宿主 Activity 解析，通常无需手动调用） |
| `start(isStart: Boolean = true)` | 开始播放。`true` 表示首次加载并注册监听，`false` 表示从暂停状态续播 |
| `pause()` | 暂停播放 |
| `resume()` | 续播（等价于 `start(false)`） |
| `isPlaying(): Boolean` | 是否正在播放 |
| `isPrepare(): Boolean` | 播放器是否已完成准备 |
| `destroy()` | 释放播放器并移除所有定时任务，**必须在 `onDestroy` 中调用** |

### 回调

| 回调 | 说明 |
| --- | --- |
| `onActionBack: (() -> Unit)?` | 点击返回按钮 |
| `onCompletion: ((IMediaPlayer) -> Unit)?` | 播放完成 |
| `onError: ((IMediaPlayer, Int, Int) -> Unit)?` | 播放出错 |

### 生命周期约定

为避免内存泄漏与状态错乱，宿主需正确对接生命周期：

```kotlin
override fun onPause() {
    super.onPause()
    videoView.pause()
}

override fun onResume() {
    super.onResume()
    if (!videoView.isPlaying() && videoView.isPrepare()) {
        videoView.resume()
    }
}

override fun onDestroy() {
    super.onDestroy()
    videoView.destroy()
}
```

### 使用示例

布局中引入：

```xml
<com.dian.demo.ui.view.video.VideoPlayerView
    android:id="@+id/video_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

代码中初始化：

```kotlin
videoView.initData()
videoView.setVideoPath("https://media.w3.org/2010/05/sintel/trailer.mp4")
videoView.setScaleType(VideoScaleType.RATIO_FILL_SIZE)
videoView.setSpeed(1f)
videoView.start()

videoView.onActionBack = { finish() }
```

> 完整示例可参考 `com.dian.demo.ui.activity.VideoPlayerActivity`。

---

## VideoPlayButton

继承自 `View` 的自定义播放 / 暂停按钮，通过 `Path` + `PathMeasure` 在「三角形（播放）」与「双竖线（暂停）」之间做形变动画，外圈伴随圆环转场。

> ⚠️ 该控件主动调用了 `setLayerType(LAYER_TYPE_SOFTWARE, null)` 关闭硬件加速，因为 `PathMeasure.getSegment` 与 `CornerPathEffect` 在硬件加速下渲染不正确，请勿删除。

### 状态常量

| 常量 | 含义 |
| --- | --- |
| `STATE_PLAY` (0) | 播放状态（显示暂停图标） |
| `STATE_PAUSE` (1) | 暂停状态（显示播放三角） |

### 公开 API

| 方法 | 说明 |
| --- | --- |
| `play()` | 切换到播放状态并执行过渡动画 |
| `pause()` | 切换到暂停状态并执行过渡动画 |
| `getCurrentState(): Int` | 获取当前状态 |
| `setAnimDuration(duration: Int)` | 设置动画时长（毫秒） |
| `setLineColor(color: Int)` | 设置线条颜色 |
| `setLineSize(size: Int)` | 设置线条粗细 |

### 自定义属性

| 属性 | 说明 | 默认值 |
| --- | --- | --- |
| `pb_lineColor` | 线条颜色 | `Color.WHITE` |
| `pb_lineSize` | 线条粗细 | `dp_4` |
| `pb_animDuration` | 动画时长（毫秒） | `200` |

---

## VideoScaleType

视频画面缩放模式枚举：

| 枚举值 | 说明 |
| --- | --- |
| `ORIGINAL_SIZE` | 原始尺寸，超出控件范围时按控件尺寸裁剪 |
| `FULL_SIZE` | 拉伸铺满整个控件（可能变形） |
| `RATIO_FILL_SIZE` | 按视频宽高比等比缩放填充（默认值） |

---

## 依赖说明

- 依赖 IjkPlayer（`tv.danmaku.ijk.media.player`）
- 亮度调节会读取 `Settings.System.SCREEN_BRIGHTNESS` 并修改宿主窗口的 `screenBrightness`，需确保宿主为 Activity（默认自动解析）
- 音量调节通过 `AudioManager` 控制 `STREAM_MUSIC`