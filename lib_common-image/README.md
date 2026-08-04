# lib_common-image

通用图片基础能力模块，默认基于 Glide 实现，同时通过 `ImageEngine` 保留替换图片加载引擎的扩展点。

## 能力边界

本模块负责：

- 图片加载：url、file、uri、resource、bitmap 到 `ImageView`
- 图片显示配置：占位图、错误图、圆角、圆形、裁剪、缩略图、缓存策略
- 图片缓存：清理内存缓存、清理磁盘缓存、暂停/恢复请求
- Bitmap 处理：采样解码、Exif 方向修正、压缩、缩放
- 文件能力：保存到 cache、复制 uri 到 cache、保存到系统相册

本模块不负责：

- 图片选择：留给 `lib-common-media-picker`
- 图片分享：留给 `lib_common-share`
- 图片上传：留给 http 或业务层
- 二维码/图像识别：留给 `lib_common-scan`

## 使用示例

```kotlin
imageView.loadImage(url) {
    placeholder(R.drawable.placeholder)
    error(R.drawable.image_error)
    centerCrop()
    radius(16)
}
```

```kotlin
CommonImage.load(
    imageView,
    url,
    ImageOptions(circleCrop = true)
)
```

```kotlin
val file = ImageFileStore.saveBitmapToCache(context, bitmap)
val galleryUri = MediaStoreSaver.saveBitmap(context, bitmap)
```

## 替换引擎

```kotlin
CommonImage.init(customImageEngine)
```

`customImageEngine` 只需要实现 `ImageEngine`，上层调用方不需要感知底层是 Glide、Coil 还是其他实现。

模块内置两个引擎，位于 `engine` 目录：

| 引擎 | 说明 |
| --- | --- |
| `GlideImageEngine` | 默认引擎，基于 Glide |
| `CoilImageEngine` | 基于 Coil |

切换到 Coil（建议在 `Application.onCreate()` 中调用一次）：

```kotlin
CommonImage.init(CoilImageEngine())
```

### 引擎能力差异

`CoilImageEngine` 受 Coil 自身能力限制，以下配置与 Glide 实现不完全等价：

| 配置 | Glide | Coil |
| --- | --- | --- |
| `thumbnail` | 先加载缩略图再加载原图 | 无对应能力，不生效 |
| `diskCacheStrategy` | 区分 DATA / RESOURCE | 只缓存原始数据，除 `NONE` 外统一启用 |
| `pause` / `resume` | 全局暂停/恢复请求 | 无对应 API，空实现；请求会在 View detach 时自动取消 |

其余配置（占位图、错误图、圆角、圆形、裁剪、内存缓存开关、渐显、指定尺寸）两个引擎行为一致。
