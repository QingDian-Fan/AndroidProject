# 需求：项目全模块混淆配置与 Release 验证

## 需求背景

当前项目由 `demo` Application、13 个 Android Library 和 2 个 Java 编译期模块组成。`demo` 的 Release 构建已经开启 R8 混淆和资源压缩，各 Android Library 也已经声明 `consumerProguardFiles`，但现有规则仍需要按模块进行完整审计和收敛。

本次重点解决以下问题：

- 确认每个参与运行的模块都有与自身职责匹配的 consumer rules。
- 避免仅在 `demo/proguard-rules.pro` 中集中保留所有模块，导致 Library 独立复用时缺少规则。
- 避免使用整包 `-keep class xxx.** { *; }` 导致核心业务代码无法正常混淆和优化。
- 保证 AspectJ AOP 织入产物在 R8 优化后仍可正常执行。
- 保证 APT 生成的登录辅助类、类名字符串和方法名字符串在混淆后仍可通过反射访问。
- 保证 ViewBinding、Retrofit、Room、Moshi/Gson、WebView JSBridge、Parcelable、分享 SDK、OpenCV 和 FFmpeg JNI 等入口不被错误裁剪或重命名。
- 建立可重复执行的 Release 构建、产物检查和功能回归标准。

## 当前项目现状

### 1. 模块清单

当前 `settings.gradle` 包含以下模块：

| 模块 | 类型 | 主要混淆风险 |
| --- | --- | --- |
| `demo` | Android Application | Manifest 入口、Scheme 反射、APT 生成代码、AOP 调用点、枚举字符串、业务模型 |
| `lib_annotation` | Java Library | APT 注解定义，仅参与编译 |
| `lib_processor` | Java Library / Annotation Processor | JavaPoet 生成类名和方法名字符串，仅参与编译 |
| `lib_common-ui` | Android Library | ViewBinding 泛型和反射、Activity/Fragment 基类、AOP 织入 |
| `lib_common-http` | Android Library | Retrofit、Room、Moshi/Gson 反射、泛型签名 |
| `lib_common-utils` | Android Library | Moshi Kotlin Metadata、反射工具、系统兼容反射 |
| `lib_common-auth` | Android Library | APT 生成类反射、登录页面类名、登录判断方法名、系统 Hook |
| `lib_common-image` | Android Library | Glide 模块发现、图片引擎公共入口 |
| `lib_common-security` | Android Library | 对外安全 API，内部实现应尽量允许混淆 |
| `lib_common-theme` | Android Library | Application 基类和主题公共入口 |
| `lib_common-weight` | Android Library | XML 自定义 View、ViewBinding、WebView JSBridge、JSON 模型、Media3 |
| `lib_common-aop` | Android Library | Aspect 类、切点注解、`AjcClosure`、`ajc$` 织入成员 |
| `lib_common-share` | Android Library | 微信、QQ、微博回调类及 Glide 集成 |
| `lib-common-media-picker` | Android Library | Activity、Parcelable、ViewBinding、AOP 权限切点、反射打开视频页 |
| `lib_common-scan` | Android Library | OpenCV JNI、微信扫码实现、native 方法名 |
| `lib_common-player` | Android Library | FFmpeg JNI 导出方法和 `onNative*` 回调 |

### 2. 当前构建配置

- `demo` 的 `release` 已配置：
  - `minifyEnabled true`
  - `shrinkResources true`
  - `proguard-android-optimize.txt`
  - `demo/proguard-rules.pro`
- `demo` 的 `debug` 保持 `minifyEnabled false`。
- 13 个 Android Library 均已声明 `consumerProguardFiles`。
- 大部分 Android Library 的自身 `release` 构建保持 `minifyEnabled false`。
- `lib_common-player` 使用 `consumer-rules.pro`，其他 Android Library 使用 `proguard-rules.pro`。
- `lib_annotation` 和 `lib_processor` 是编译期 Java 模块，不直接进入 APK 运行时，不应机械套用 Android Library 的 `consumerProguardFiles`。

### 3. 当前重点风险

- 部分模块使用整包 `-keep`，可能导致整个模块无法混淆或优化。
- `demo/proguard-rules.pro` 存在部分与 Library consumer rules 重复的规则，需要明确规则归属并避免无意义重复。
- AOP 在 `demo`、`lib_common-aop`、`lib_common-ui` 和 `lib-common-media-picker` 中执行织入，R8 必须处理织入后的 class，而不是未织入的 class。
- AOP 切面会在运行期读取 `@SingleClick`、`@CheckPermissions` 等方法注解，必须保留相应 Annotation 属性和必要成员。
- APT 生成 `com.dian.demo.apt.AndLoginUtils`，`LoginHookUtil` 使用 `Class.forName()` 和 `getMethod()` 反射调用：
  - `getLoginActivity()`
  - `getRequireLoginList()`
  - `getJudgeLoginMethod()`
- APT 生成代码中包含 Activity 完整类名以及 `类名#方法名` 字符串，R8 不得让字符串与实际混淆名称失配。
- `@RequireLogin` 使用 `RetentionPolicy.SOURCE`，不能依赖 `-keep @RequireLogin class *` 在 R8 阶段匹配目标类。
- `LoginPage` 和 `CheckLogin` 当前为 Runtime 注解，但 `lib_annotation` 在 `demo` 中使用 `compileOnly`，需要确认最终方案是否真的依赖运行期注解，不得留下仅表面存在但实际无效的规则。
- `SchemeUtils`、`ImagePreviewActivity` 等位置通过类名和方法名反射打开页面，需要保持目标类名和入口方法一致。
- `ViewBindingReflect` 依赖泛型签名、Binding 类和静态 `bind()`/`inflate()` 方法。
- `lib_common-player` 的 JNI 使用固定 Java 类名、native 方法名和 `onNative*` 回调方法名。
- `lib_common-scan` 中 OpenCV Java API 包含大量 JNI 方法，类名和成员名不能被错误改写。

## 需求目标

1. 最终由 `demo` Release 的 R8 对 Application 和所有运行时 Library 统一执行压缩、优化和混淆。
2. 每个 Android Library 在自己的 consumer rules 中声明自身必需的保留规则，使模块被其他宿主引用时仍能正常运行。
3. 编译期模块与运行时模块分开处理，不为 `lib_annotation`、`lib_processor` 添加无效 Android 混淆配置。
4. 在保证反射、AOP、APT、JNI 和第三方 SDK 正常运行的前提下，尽可能允许内部实现被裁剪、优化和重命名。
5. Release 构建不得依靠 `-dontobfuscate`、`-dontshrink`、`-dontoptimize` 或全局 `-ignorewarnings` 规避问题。
6. 所有规则必须有明确代码依据，并归属到实际提供该能力的模块。

## 配置原则

### 1. Application 与 Library 职责

- `demo/proguard-rules.pro` 只负责 Application 自身入口、业务反射和最终集成兜底规则。
- Android Library 的规则应写入各自的 consumer rules，并通过 `consumerProguardFiles` 随 AAR 传递给宿主。
- 不要求每个 Library 单独开启 `minifyEnabled true`；最终 APK 中的 Library 字节码由 `demo` Release 的 R8 统一处理。
- 如果 Library 的 Release 构建保持 `minifyEnabled false`，仍必须验证 consumer rules 已正确打入 AAR。
- 第三方 AAR 已提供 consumer rules 时，不重复添加无依据的整包保留规则。

### 2. 最小保留原则

- 优先使用：
  - `-keepnames`
  - `-keepclassmembers`
  - `-keepclasseswithmembers`
  - 基于注解、继承关系或接口的精确规则
- 只有明确存在 JNI、外部 SDK 回调、跨应用反射或稳定公共二进制名称要求时，才允许完整 `-keep`。
- 不得为了让 Release 构建通过而直接保留整个 Application 或所有 common 模块。
- 每一条 `-dontwarn` 必须对应确定的可选依赖或已知无运行路径的引用，并添加注释说明。

### 3. 通用属性保留

根据实际使用统一保留以下元数据：

- `Signature`
- Runtime Annotation 相关属性
- `InnerClasses`
- `EnclosingMethod`
- `AnnotationDefault`
- 用于 Release 崩溃定位的 `SourceFile` 和 `LineNumberTable`

不得无依据保留所有调试信息或全部类成员。

## 分模块功能要求

### 1. `demo`

- 保持 Release 开启 R8、代码压缩、优化、混淆和资源压缩。
- 保持 Debug 默认不混淆，避免影响日常调试。
- 保留 Manifest 注册的 Application、Activity、Service、Receiver、Provider 等必要入口。
- 保留通过 `SchemeUtils` 类名字符串打开的页面名称和对应 `start(Context, ...)` 入口。
- 保留通过 `ImagePreviewActivity` 反射调用的 `VideoPlayerActivity.start(Context, String)`。
- 保证 `AudioEngineType.name` 与 `valueOf()` 的存取逻辑在 Release 下保持一致。
- 不得通过保留整个 `com.demo.project.ui.**` 包解决个别反射入口问题；应按入口类型和方法精确配置。
- `demo` 不重复维护已经由各 Library consumer rules 提供的通用规则，必要的集成兜底规则必须注明原因。

### 2. `lib_annotation`

- 明确该模块仅作为编译期注解依赖，不直接作为运行时 Android Library 配置。
- 核对 `RequireLogin`、`LoginPage`、`CheckLogin` 的 Retention 是否与实际使用方式一致。
- 不允许在 R8 规则中依赖 `SOURCE` 注解匹配被注解类。
- 如果某个注解只供 APT 使用，应按编译期语义处理；如果确需运行期反射，必须确保注解类进入 APK 并保留 Annotation 属性。

### 3. `lib_processor`

- 保证 `RequireLoginProcessor` 能被 AutoService 正确注册并在 KAPT 阶段执行。
- 保证生成的 `com.dian.demo.apt.AndLoginUtils` 类名稳定。
- 保证以下生成方法不会被裁剪或重命名：
  - `getLoginActivity()`
  - `getRequireLoginList()`
  - `getJudgeLoginMethod()`
- 生成代码中的 Activity 类名和登录判断方法名必须与 Release 实际名称一致。
- 不得仅依赖 `@RequireLogin` 的 R8 注解匹配规则保护目标 Activity，因为该注解为 `SOURCE` 保留策略。
- 可采用精确 keep names、R8 可识别的直接类型引用或由处理器生成配套规则等方式解决，但不得改变现有登录业务语义。
- Processor 自身不进入 APK，不需要为处理器实现类添加运行时 keep。

### 4. `lib_common-aop`

- 保留 AspectJ 运行所需的 Aspect 类、切点信息、注解属性和织入辅助成员。
- 正确处理 `AjcClosure`、`ajc$*` 等织入产物。
- 保证以下切面在 Release 下正常生效：
  - `SingleClickAspect`
  - `CheckNetAspect`
  - `CheckPermissionsAspect`
  - `ViewAspect`
  - `AndroidAspect`
- 保证切面运行期读取的方法注解不会被裁剪。
- 不得使用保留整个 `com.common.aop.**` 所有成员作为默认方案；应区分注解、Aspect 类和普通实现类。
- 保留必要的 `org.aspectj` 告警抑制，但不得掩盖真实缺失依赖或织入失败。

### 5. `demo`、`lib_common-ui` 与 `lib-common-media-picker` 的 AOP 织入

- 核对 `gradle/aspectj-weave.gradle` 的织入顺序，确保 Java/Kotlin 编译产物先完成织入，再交给 R8。
- 保持现有 `r8OutputWorkaround` 与当前 AGP 8.13.0 的 Release R8 任务兼容。
- 保证 Kotlin 和 Java 调用点均按各模块配置正确织入。
- 保证 `lib-common-media-picker` 中 `@CheckPermissions` 在 Release 下仍会先执行权限检查。
- 不得因为 R8 优化而删除切面调用、运行期注解或织入辅助类。
- Release 构建中不得出现 AspectJ error、abort 或 weave failure。

### 6. `lib_common-auth`

- 保留 APT 生成登录辅助类的名称和反射调用方法。
- 保留生成代码字符串引用的登录页类名、需登录页面类名和登录判断方法名。
- Android Framework 内部类的反射名称不需要通过保留项目业务类解决。
- 不得整包保留所有认证实现；公共 API 与反射入口之外的内部实现应允许混淆。
- Release 下必须验证未登录拦截、跳转登录页、登录状态判断和登录后回跳链路。

### 7. `lib_common-ui`

- 保留 ViewBinding 泛型解析所需的 `Signature`。
- 保留生成的 `*Binding` 类及静态 `bind()`、`inflate()` 方法。
- 保证 `BaseAppBindActivity`、`BaseAppVMActivity`、`BaseAppBindFragment` 和 `BaseAppVMFragment` 的泛型绑定关系可被 `ViewBindingReflect` 正确解析。
- 不得默认完整保留所有基类子类的全部成员；只保留反射实际依赖的类型、签名和方法。
- 保证 AOP 织入后的 UI 基类在 Release 下正常工作。

### 8. `lib_common-http`

- 保留 Retrofit 接口方法注解、泛型签名和 suspend 相关签名。
- 保证 `RequestService` 能由 Retrofit 正确创建。
- 保证 Room Database、DAO、Entity、TypeConverter 及生成实现可正常工作。
- 保证 Moshi/Gson 反射模型的字段、构造方法、泛型和 Kotlin Metadata 不被错误裁剪。
- 对带 `@SerializedName`、`@JsonAdapter` 等注解的字段优先采用注解成员规则，不整包保留全部模型。
- 审计 `APIConfig` 中通过固定类名和方法名进行的反射；无真实运行路径的示例反射不得成为整包 keep 的理由。
- `-dontwarn` 仅用于 OkHttp 可选 TLS Provider 等确定的可选依赖。

### 9. `lib_common-utils`

- 保留自定义 Moshi Kotlin Adapter 所需的 Kotlin Metadata、泛型和目标模型信息。
- 保证 `Stroke` 等实际反射序列化模型正常解析。
- 对 Android Framework、MIUI、Flyme 等系统类的反射无需保留项目业务类。
- 不得因为系统兼容反射而整包保留 `com.common.utils`。

### 10. `lib_common-weight`

- 保留 XML 布局实例化自定义 View 所需的构造方法。
- 保留 WebView `@JavascriptInterface` 方法。
- 保证 WebView command、dispatcher 和 JSON payload 模型在 Release 下正常分发与解析。
- 保证 Media3/ExoPlayer 使用其官方 consumer rules，不无依据整包保留 Media3。
- View、WebView 和视频播放器的普通内部实现应允许混淆。

### 11. `lib_common-image`

- 保证 Glide Module、GeneratedAppGlideModule 和 RequestManager 工厂可以被正确发现。
- 保证 Glide、Coil 图片引擎公共调用入口正常。
- 不得默认完整保留所有 `com.common.image.**` 实现；应仅保留确有 Java API、反射或生成代码要求的入口。
- Release 下验证 Glide 与 Coil 的普通加载、占位图、错误图、圆角和缩放功能。

### 12. `lib_common-security`

- 对宿主必须稳定访问的公共安全 API 保留必要名称或成员。
- 加密、摘要等内部实现应允许 R8 混淆和优化。
- 不得使用整包完整保留作为默认配置，除非存在明确外部反射或二进制兼容要求。

### 13. `lib_common-theme`

- 保留主题 Application 基类和由 Manifest、反射或 XML 直接引用的必要入口。
- 如果模块不存在额外反射或生成代码，仅保留通用属性即可，不为满足“每个模块有规则”而添加无效整包 keep。
- Release 下验证主题初始化和换肤流程。

### 14. `lib_common-share`

- 保证 QQ、微信、微博 SDK 的 Activity、回调和反射入口正常。
- 优先采用第三方 SDK 官方 consumer rules。
- 仅在官方规则缺失且有实际依据时补充 Tencent、Weibo 或 Glide 相关规则。
- 不得长期整包保留全部第三方 SDK 而不验证必要性。
- Release 下验证分享调起、授权回调、取消和失败回调。

### 15. `lib-common-media-picker`

- 保证 Manifest Activity、Parcelable Creator、ViewBinding 和图片预览相关入口正常。
- 保证 `ImageSelectActivity.start()` 上的 `@CheckPermissions` 在 Release 下正常织入和执行。
- 保证反射调用 `VideoPlayerActivity.start(Context, String)` 不因混淆失败。
- 不得完整保留整个媒体选择模块的全部实现；适配器和内部页面成员应在不影响入口的情况下允许混淆。

### 16. `lib_common-scan`

- 保留 OpenCV Java/JNI 对应的类名和 native 方法名。
- 保证 `libopencv_java4.so` 与 Java API 的符号对应关系不被破坏。
- 保证微信二维码扫描、相机预览和扫码回调正常。
- 对 `com.common.scan.**` 的整包 keep 进行必要性审计；仅 JNI、反射、Manifest 和公共回调入口需要保留。
- 保持当前 ABI 和 native 库打包配置不变。

### 17. `lib_common-player`

- 保留 JNI 导出符号依赖的 Java 类名和 native 方法名。
- 保留 C++ 通过 `GetMethodID` 查找的 `onNative*` 回调方法名和签名。
- 保证 `FfmpegAudioPlayer`、`FfmpegVideoPlayer` 与 native 实现的签名完全一致。
- 不得裁剪 native 方法或仅由 JNI 回调的方法。
- 在满足 JNI 映射的前提下，允许与 JNI 无关的普通辅助代码被混淆。
- 保持当前 `arm64-v8a`、`armeabi-v7a` ABI 和 FFmpeg 库加载逻辑不变。

## 第三方依赖要求

需要检查以下依赖是否已经提供官方 consumer rules：

- AndroidX / Material / Navigation
- Retrofit / OkHttp / Okio
- Room
- Moshi / Gson
- Media3 / ExoPlayer
- Glide / Coil
- CameraX
- Chucker
- PhotoView
- QQ / 微信 / 微博 SDK
- OpenCV
- AspectJ Runtime

处理要求：

- 已有官方规则时优先使用官方规则。
- 不复制过时或与当前版本不匹配的网络示例规则。
- 不使用全局 `-dontwarn **`。
- Release 构建产生的缺失类告警必须逐条确认来源和运行路径。

## 允许修改范围

- `demo/build.gradle`
- `demo/proguard-rules.pro`
- 各 Android Library 的 `build.gradle`
- 各 Android Library 的 `proguard-rules.pro` 或 `consumer-rules.pro`
- `gradle/aspectj-weave.gradle`
- `lib_annotation` 中与 APT/R8 语义直接相关的注解 Retention 配置
- `lib_processor` 中与生成类、生成方法或混淆规则产出直接相关的代码
- 与 Release 混淆验证直接相关的测试或验证脚本

只有在无法通过精确 R8 规则解决字符串反射问题时，才允许小范围调整对应反射入口；不得借机重构业务代码。

## 禁止修改范围

- 不修改业务流程、页面交互、接口协议或数据结构。
- 不升级 AGP、Gradle、Kotlin、AspectJ 或第三方依赖版本。
- 不修改 SDK、minSdk、targetSdk、ABI、签名、发布或上传配置。
- 不修改 native C++ 业务实现或预编译 `.so`。
- 不新增第三方混淆插件。
- 不关闭 Release 混淆、优化或资源压缩。
- 不启用全局 `-dontobfuscate`、`-dontshrink`、`-dontoptimize` 或 `-ignorewarnings`。
- 不使用 `-keep class com.demo.** { *; }` 或 `-keep class com.common.** { *; }` 作为兜底。
- 不删除 AOP、APT、反射、JNI 或第三方 SDK 功能来规避混淆问题。
- 不读取、输出或修改签名密码、Token、Cookie、服务地址等敏感配置。
- 不覆盖用户已有未提交修改。

## 验收标准

### 1. 配置完整性

1. `demo` Release 保持 `minifyEnabled true` 和 `shrinkResources true`。
2. 13 个 Android Library 均通过 `consumerProguardFiles` 发布与自身能力匹配的规则。
3. `lib_annotation` 和 `lib_processor` 的编译期定位有明确说明，不添加无效 Android consumer rules。
4. 所有规则均包含用途注释，并能对应到具体反射、注解、JNI、XML、Manifest 或 SDK 入口。
5. 不存在无依据的整个 Application 或整个 common 根包保留规则。

### 2. Release 构建

至少执行：

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew :demo:assembleRelease
```

同时根据修改范围执行受影响 Library 的 Release 构建，例如：

```bash
JAVA_HOME=/path/to/jdk-17 ./gradlew :lib_common-aop:assembleRelease
JAVA_HOME=/path/to/jdk-17 ./gradlew :lib_common-auth:assembleRelease
JAVA_HOME=/path/to/jdk-17 ./gradlew :lib_common-http:assembleRelease
JAVA_HOME=/path/to/jdk-17 ./gradlew :lib_common-player:assembleRelease
```

验收要求：

- R8 构建成功。
- 不存在未处理的 Missing class、AspectJ error、APT 生成失败或 JNI keep 告警。
- 不通过全局忽略告警让构建假通过。
- Release APK 能正常安装和启动。

### 3. R8 产物检查

检查 `demo/build/outputs/mapping/release/` 下实际生成的 R8 文件，包括：

- `mapping.txt`
- `usage.txt`
- `seeds.txt`
- `configuration.txt`

验收要求：

- 必须生成 `mapping.txt`，并由发布流程安全留存，不提交包含敏感路径的本地产物。
- APT 生成助手、JNI 入口、JSBridge 和必要反射入口出现在保留结果中。
- 与反射、JNI、AOP 无关的内部实现应能在 `mapping.txt` 中看到重命名结果。
- `usage.txt` 中不得包含实际运行必需但被错误裁剪的类。
- `configuration.txt` 能确认各 Library consumer rules 已合并到最终 Release 配置。

### 4. AOP 验收

Release APK 中逐项验证：

1. `@SingleClick` 能阻止规定时间内的重复点击。
2. `@CheckNet` 在无网络时能执行既有拦截逻辑。
3. `@CheckPermissions` 能正常申请权限，并在授权后继续执行原方法。
4. View 点击相关切面正常执行，不出现重复织入。
5. AOP 方法注解、Aspect 类和织入辅助类未被错误裁剪。
6. 日志或字节码检查能够证明 Release 使用的是织入后的 class。

### 5. APT 登录链路验收

Release APK 中逐项验证：

1. KAPT 成功生成 `com.dian.demo.apt.AndLoginUtils`。
2. `Class.forName()` 能找到生成类。
3. `getLoginActivity()` 能返回并打开正确登录页。
4. `getRequireLoginList()` 返回的类名与混淆后的实际页面保持匹配。
5. `getJudgeLoginMethod()` 返回的方法能够被反射调用。
6. 未登录访问受保护页面时能够正确跳转登录页。
7. 登录成功后能够回到原目标页面。
8. 不依赖 `SOURCE` 注解的 R8 匹配规则实现保留。

### 6. 核心功能回归

Release APK 至少验证：

- Application 正常初始化，首页和主要 Activity 可打开。
- Scheme 类名路由和反射 `start()` 方法正常。
- BaseActivity/BaseFragment 的 ViewBinding 页面正常创建。
- Retrofit 请求创建、Moshi/Gson 解析和 Room 基础读写正常。
- WebView JSBridge 和 command 分发正常。
- Glide、Coil 图片加载正常。
- QQ、微信、微博分享调起和回调正常。
- 图片选择、预览和 Parcelable 参数传递正常。
- CameraX 预览、OpenCV/微信二维码扫描正常。
- ExoPlayer 音视频播放正常。
- FFmpeg 音频和视频在匹配 ABI 真机上正常播放，JNI 回调无异常。
- 主题、安全工具及其他公共模块入口正常。

### 7. 混淆有效性

- Release APK 中普通业务内部类和方法已被实际重命名。
- 不应因过宽 `-keep` 导致某个普通 common 模块整体保持原名。
- 反射、JNI、AOP、APT 和 SDK 必需入口保持稳定。
- Release 包体资源压缩仍然生效。
- Debug 构建行为保持不变。

## 非本期范围

- AGP、Gradle、Kotlin 或依赖版本升级。
- 商业加固、壳保护、字符串加密或资源加密。
- Native C++ 符号混淆。
- 签名、渠道、发布和上传流程调整。
- 为缩小包体进行业务功能删除或模块拆分。
- 与混淆无关的代码重构、性能优化或 UI 修改。
