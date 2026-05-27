# lib_common-http

`lib_common-http` 是公共网络模块，类型为 Android Library，命名空间为 `com.common.http`。它封装 Retrofit、OkHttp、Cookie、缓存、下载、Room 和数据解析能力。

## 模块定位

- 提供统一的 HTTP Client 配置和请求执行入口。
- 统一请求头、公共参数、缓存、Cookie、日志和 curl 输出。
- 支撑文件下载、下载任务持久化和调试数据库查看。

## 主要能力

- Retrofit/OkHttp: 请求创建、拦截器和 Client 配置。
- 拦截器: Header、公共参数、在线缓存、离线缓存、Curl 日志。
- Cookie: Cookie 保存、读取、持久化和请求自动注入。
- 数据解析: Moshi、Gson、空值安全适配。
- 下载: `SingleDownloader`、下载任务实体和 Room 数据库。
- 调试: Debug 构建集成 Chucker 和 debug-db。

## 依赖关系

- 依赖 `lib_common-utils`
- 依赖 `lib_common-theme`
- 使用 Retrofit、OkHttp、Moshi、Room、Chucker 等第三方库。

## 使用建议

- 业务接口定义应放在使用方模块，通用 Client 和拦截器留在本模块。
- 公共参数、Header、Cookie 等全局策略应集中维护，避免业务层重复拼接。
- Release 构建使用 Chucker no-op，避免调试能力进入正式包。
