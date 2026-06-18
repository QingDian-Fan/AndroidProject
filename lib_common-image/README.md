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
