# lib_common-utils

`lib_common-utils` 是公共工具模块，类型为 Android Library，命名空间为 `com.common.utils`。它沉淀应用中跨页面、跨模块复用的工具能力。

## 模块定位

- 提供基础工具类、扩展函数和轻量功能封装。
- 为 UI、网络、分享、AOP 等模块提供通用支撑。
- 统一权限申请、缓存清理、日志、Toast、资源读取、键盘处理、二维码、DataStore、音频处理等能力。

## 主要能力

- 权限: `LivePermissions`、权限转换、默认权限说明弹窗。
- 存储与缓存: `CacheUtil`、`DataStoreExts`、`AppDataStore`、Proto serializer。
- UI 工具: Toast、状态栏、键盘、刷新、Span、资源读取、分割线。
- 网络辅助: 网络状态判断、域名处理。
- 编码与安全: MD5、RSA、AES。
- 设备与系统: 设备 ID、签名、渠道读取、Intent 工具。
- 媒体能力: 音频录制/播放、PCM/WAV 转换。
- 二维码: ZXing 编码、解码和扫码 View。
- 扩展函数: View、Dialog、Observer、布局和常用工具扩展。

## 依赖关系

- 依赖 `lib_common-theme`。
- 引入 DataStore、protobuf-javalite、ZXing、Moshi、Walle、SmartRefreshLayout 等能力。

## 使用建议

- 新增工具要避免依赖业务页面和上层模块。
- 如果工具只服务网络、UI 或分享，优先放到对应 common 模块，避免 utils 无限膨胀。
- 默认返回值和空值保护应在工具层处理，减少调用方崩溃风险。
