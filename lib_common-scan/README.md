# lib_common-scan 扫码组件

基于 **CameraX** + **OpenCV** 封装的相机二维码扫描库，内置两套识别引擎，开箱即用，同时支持高度自定义扫码界面。

> 本模块源码改编自 [Jenly1314](https://github.com/jenly1314) 的开源扫码库（WeChatQRCode / OpenCV 系列），并随包内置了 OpenCV 4.x 的 Java SDK 与原生库。

---

## 功能特性

- **两套识别引擎**
  - **微信二维码引擎**（`wechat_qrcode`）：基于深度学习模型，识别率高，支持**远距离 / 小尺寸 / 变形 / 多码**识别，并可返回每个二维码的顶点坐标。
  - **OpenCV 原生引擎**（`QRCodeDetector`）：OpenCV 自带检测器，无需额外模型文件。
- 基于 CameraX 的相机预览与帧分析，自动申请相机权限。
- 手电筒开关、**环境光自动开灯**（暗光下提示/自动点亮闪光灯）。
- **手势缩放**：双指捏合缩放、双击缩放。
- 识别成功**蜂鸣 + 震动**反馈。
- 高度可定制的 **扫码框 `ViewfinderView`**（扫描线/网格/边角/提示文字/结果点等数十个 XML 属性）。
- 多码场景下可在预览图上**标记结果点**，点击对应结果点返回该码内容。
- 提供 `Activity` / `Fragment` 两种基类，便于自定义布局与交互。

---

## 依赖与环境

- `minSdk 24`，CameraX `1.5.x`（以 `api` 暴露，使用方可直接引用 CameraX）。
- 内置 OpenCV：
  - Java SDK 源码：`src/main/java/org/opencv/**`
  - 原生库：`src/main/jniLibs/{arm64-v8a, armeabi-v7a, x86, x86_64}/libopencv_java4.so`
  - 微信引擎模型文件：`src/main/assets/models/`（`detect.prototxt/caffemodel`、`sr.prototxt/caffemodel`）
- 已在模块 `AndroidManifest.xml` 声明权限：`CAMERA`、`VIBRATE`、`FLASHLIGHT`。
- `build.gradle` 中通过 `packagingOptions { doNotStrip '**/*.so' }` 保留 OpenCV 原生符号，请勿删除。

在使用方模块依赖本库：

```groovy
implementation project(":lib_common-scan")
```

---

## 快速开始

### 1. 初始化（仅微信引擎需要）

微信引擎首次使用前需把模型文件从 `assets` 拷贝到外部私有目录并加载，建议在 `Application` 中初始化一次：

```kotlin
class ProjectApplication : BaseApplication() {
    override fun onCreate() {
        super.onCreate()
        WeChatQRCodeDetector.init(this) // 内部完成模型拷贝 + OpenCV 加载
    }
}
```

> OpenCV 原生库通过 `OpenCVLoader.initLocal()` 加载（见 `org.opencv.OpenCV`）。OpenCV 引擎（`OpenCVQRCodeActivity`）不依赖上面的模型文件。

### 2. 启动扫码并接收结果

模块已内置三个可直接使用的扫码页（均已在 Manifest 注册，竖屏 + `CameraScanTheme`）：

| Activity | 引擎 | 说明 |
| --- | --- | --- |
| `WeChatQRCodeActivity` | 微信 | 单码扫描示例，多码时在预览图标记结果点供点击选择 |
| `WeChatMultiQRCodeActivity` | 微信 | 多码识别示例（一次返回多个结果） |
| `OpenCVQRCodeActivity` | OpenCV | OpenCV 原生检测器示例 |

启动并解析结果（结果通过 `CameraScan.SCAN_RESULT` 回传）：

```kotlin
private val scanLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        val content = CameraScan.parseScanResult(result.data) // 取扫码文本
        content?.let { /* 处理结果 */ }
    }
}

// 触发扫码
scanLauncher.launch(Intent(this, WeChatQRCodeActivity::class.java))
```

相机权限会由扫码页基类自动申请，无需调用方处理。

---

## 自定义扫码页

通过继承基类实现自己的扫码界面：

- 微信引擎：继承 `WeChatCameraScanActivity` / `WeChatCameraScanFragment`
- OpenCV 引擎：继承 `OpenCVCameraScanActivity` / `OpenCVCameraScanFragment`
- 通用基类：`BaseCameraScanActivity<T>` / `BaseCameraScanFragment<T>`

常用可重写方法（来自 `BaseCameraScanActivity`）：

| 方法 | 默认值 | 说明 |
| --- | --- | --- |
| `getLayoutId()` | `R.layout.camera_scan` 等 | 自定义扫码页布局 |
| `getPreviewViewId()` | `R.id.previewView` | CameraX `PreviewView` 的 id |
| `getFlashlightId()` | `R.id.ivFlashlight` | 手电筒按钮 id，返回 `View.NO_ID` 则不显示 |
| `getViewfinderViewId()` | `R.id.viewfinderView` | 扫码框 id（WeChat/OpenCV 基类） |
| `createAnalyzer()` | — | 返回帧分析器（见下） |
| `createCameraScan(previewView)` | `BaseCameraScan` | 自定义相机控制实现 |
| `onScanResultCallback(result)` | — | **识别结果回调，必须实现** |

示例：

```kotlin
class MyScanActivity : WeChatCameraScanActivity() {

    override fun getLayoutId() = R.layout.activity_wechat_qrcode

    // 分析器：构造参数 isOutputVertices=true 时会额外返回二维码顶点坐标
    override fun createAnalyzer(): Analyzer<MutableList<String>> =
        WeChatScanningAnalyzer(true)

    override fun onScanResultCallback(result: AnalyzeResult<List<String>>) {
        cameraScan.setAnalyzeImage(false) // 先停止分析
        val intent = Intent().putExtra(CameraScan.SCAN_RESULT, result.result[0])
        setResult(RESULT_OK, intent)
        finish()
    }
}
```

### `CameraScan<T>` 相机控制能力

`cameraScan`（基类成员）可链式配置：

```kotlin
cameraScan
    .setAnalyzeImage(true)        // 开/关帧分析（暂停/继续扫码）
    .setAutoStopAnalyze(true)     // 识别到结果后自动停止分析
    .setPlayBeep(true)            // 识别成功蜂鸣
    .setVibrate(true)             // 识别成功震动
    .setNeedTouchZoom(true)       // 手势缩放
    .setDarkLightLux(45f)         // 暗光阈值（自动点亮闪光灯）
    .setBrightLightLux(100f)      // 亮光阈值（自动熄灭）
    .bindFlashlightView(view)     // 绑定手电筒按钮
```

---

## 扫码框 ViewfinderView

`com.common.scan.view.ViewfinderView` 是一个可在 XML 中高度定制的扫码遮罩层，常用属性（完整见 `res/values/attrs.xml`）：

| 属性 | 说明 |
| --- | --- |
| `vvViewfinderStyle` | `classic`（经典，挖空取景框）/ `popular`（全屏流行样式） |
| `vvMaskColor` / `vvFrameColor` | 遮罩 / 边框颜色 |
| `vvLaserStyle` | 扫描线样式：`none` / `line` / `grid` / `image` |
| `vvLaserColor` / `vvLaserMovementSpeed` | 扫描线颜色 / 移动速度 |
| `vvFrameCornerColor` / `vvFrameCornerSize` | 取景框四角颜色 / 长度 |
| `vvLabelText` / `vvLabelTextColor` / `vvLabelTextLocation` | 提示文字及位置 |
| `vvPointColor` / `vvPointDrawable` / `vvPointAnimation` | 多码结果点样式与动画 |

代码侧常用：`showScanner()`（显示扫描动画）、`showResultPoints(points)`（显示结果点）、`isShowPoints`、`setOnItemClickListener { index -> }`（点击结果点回调）。

---

## 目录结构

```
com/common/scan/
├── WeChatQRCodeActivity.kt        // 微信引擎-单码示例页
├── WeChatMultiQRCodeActivity.kt   // 微信引擎-多码示例页
├── OpenCVQRCodeActivity.kt        // OpenCV 引擎示例页
├── Function.kt                    // Bitmap 画框、Uri 转 Bitmap 等扩展
├── camera/                        // 相机抽象与基类
│   ├── CameraScan / BaseCameraScan          // 相机控制抽象 + CameraX 实现
│   ├── BaseCameraScanActivity / Fragment    // 扫码页基类（预览 + 手电筒 + 权限）
│   ├── analyze/Analyzer                     // 帧分析器接口
│   ├── AnalyzeResult / FrameMetadata        // 分析结果 / 帧元数据
│   ├── config/                              // 相机参数策略（分辨率/比例/自适应）
│   ├── manager/                             // AmbientLightManager 光感、BeepManager 蜂鸣
│   ├── internal/ZoomGestureDetector         // 缩放手势
│   └── util/                                // ImageUtils(YUV→NV21)/PointUtils 坐标换算等
├── wechat/                        // 微信引擎：CameraScanActivity/Fragment、Detector、Analyzer
├── opencv/                        // OpenCV 引擎：CameraScanActivity/Fragment、Detector、Analyzer
└── view/ViewfinderView            // 扫码取景框

org/opencv/**                      // 随包内置的 OpenCV 4.x Java SDK
assets/models/**                   // 微信引擎深度学习模型
jniLibs/**/libopencv_java4.so      // OpenCV 原生库
```

---

## 静态识别（图片解码）

除相机实时扫描外，可直接对 `Bitmap` 解码（微信引擎）：

```kotlin
// 仅文本
val results: List<String> = WeChatQRCodeDetector.detectAndDecode(bitmap)

// 文本 + 顶点坐标
val points = ArrayList<Mat>()
val results = WeChatQRCodeDetector.detectAndDecode(bitmap, points)
```

---

## 注意事项

- **混淆**：`minifyEnabled` 下请确保 OpenCV 包（`org.opencv.**`）与 native 方法不被裁剪；模块已配置 `doNotStrip` 保留 `.so`。
- **包体积**：内置四个 ABI 的 `libopencv_java4.so` 与模型文件，体积较大。如只需部分架构，可在使用方 `ndk { abiFilters ... }` 中裁剪。
- 微信引擎依赖 `WeChatQRCodeDetector.init(context)` 完成模型加载，未初始化时识别会失败。
- 内置扫码页固定竖屏（`screenOrientation="portrait"`）。
