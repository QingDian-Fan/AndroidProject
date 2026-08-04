## 需求：图片加载库扩展Bug处理

## 需求背景

### P2 中

- 严重等级：P2 中
- 文件路径：[CoilImageEngine.kt](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/lib_common-image/src/main/java/com/common/image/engine/CoilImageEngine.kt:106)
- 代码位置：`applyScaleType()`、`applyTransformations()`，106–135 行
- 问题描述：代码为 `FIT_CENTER`/`CENTER_INSIDE` 设置了 `Scale.FIT`，但随后使用的 Coil 2.7 `RoundedCornersTransformation` 会在变换内部按目标尺寸使用 `Scale.FILL` 居中裁剪。因此“缩放模式 + 圆角”的组合与现有 Glide 引擎不等价，也与 README 中“圆角、裁剪行为一致”的说明冲突。
- 触发条件：启用 `CoilImageEngine`，加载宽高比与 ImageView 不一致的图片，同时配置 `fitCenter()` 或 `centerInside()` 以及 `radius(...)`。
- 实际影响：原本应完整显示的图片会被居中裁掉；`centerInside` 场景还可能把小图放大并裁剪，导致切换图片引擎后出现明显显示回归。
- 修复建议：使用能够遵循 `ImageScaleType` 的自定义圆角变换，分别实现 FIT/FILL/CENTER_INSIDE 语义；或者改用不改变图片缩放结果的圆角裁剪方案。应补充不同宽高比下“缩放模式 + 圆角”的测试。

