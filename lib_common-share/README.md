# lib_common-share

`lib_common-share` 是公共分享模块，类型为 Android Library，命名空间为 `com.common.share`。它封装 QQ、微信、微博和自定义渠道分享能力。

## 模块定位

- 统一第三方平台分享入口。
- 提供分享弹窗、平台列表、分享数据模型和渠道执行逻辑。
- 为业务模块屏蔽不同平台 SDK 的调用差异。

## 主要能力

- `ShareActivity`: 分享结果回调基础 Activity。
- `ShareFactory`: 渠道创建和分享成功广播。
- `ShareModel`: 分享内容数据模型。
- `ShareDialog`: 平台选择弹窗。
- `QQChannel`、`WeChatChannel`、`WeiBoChannel`、`CustomChannel`: 不同分享渠道实现。
- `FileShareHelper`、`ShareUtils`: 文件分享和通用分享工具。

## 依赖关系

- 依赖 `lib_common-utils`
- 依赖 `lib_common-theme`
- 使用 QQ、微信、微博 SDK。
- 使用 Glide 加载分享图片和平台图标。

## 使用建议

- 新增平台时优先扩展 `Channel` 体系，避免在业务页面中直接调用平台 SDK。
- 分享内容统一通过 `ShareModel` 传递，减少字段散落。
- 第三方 SDK 的 appId、回调 Activity 和 manifest 配置应由接入应用统一确认。
