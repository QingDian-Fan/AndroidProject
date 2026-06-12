# lib_common-theme

`lib_common-theme` 是公共主题和 Application 基础模块，类型为 Android Library，命名空间为 `com.common.theme`。

## 模块定位

- 提供 common 体系中最底层的主题和 Application 基类能力。
- 作为 `lib_common-utils`、`lib_common-ui`、`lib_common-http`、`lib_common-share` 等模块的基础依赖。

## 主要内容

- `BaseApplication`: 公共 Application 基类，用于保存应用上下文和承接全局初始化扩展。
- `NightModeManager`: 基于 AndroidX `AppCompatDelegate` 的日夜间模式管理，支持跟随系统、日间、夜间三种模式。
- `AppLanguageManager`: 基于 AndroidX `AppCompatDelegate` 的应用语言管理，支持跟随系统、中文、英文三种模式。
- `res/values` 与 `res/values-night`: 公共颜色、主题、资源占位和夜间模式基础资源。

## 依赖关系

该模块不依赖其他项目内模块，只依赖 AndroidX、AppCompat 和 Material 等基础库。

## 使用建议

- 公共主题、颜色和基础 Application 能力优先放在该模块。
- 不建议在这里引入网络、业务或复杂 UI 依赖，避免底层模块变重。
- 日夜间资源遵循 Android 原生规则：默认资源放在 `res/values`、`res/drawable`，夜间覆盖资源放在 `res/values-night`、`res/drawable-night`，文件名和资源名保持一致即可。
- 切换模式时调用 `NightModeManager.setMode(context, NightMode.FOLLOW_SYSTEM)`、`NightMode.DAY` 或 `NightMode.NIGHT`；系统会自动重建界面并按资源限定符选择对应资源，不需要在布局里写 `tag`。
- 切换语言时调用 `AppLanguageManager.setLanguage(context, AppLanguage.FOLLOW_SYSTEM)`、`AppLanguage.CHINESE` 或 `AppLanguage.ENGLISH`；默认文案放在 `res/values`，英文文案放在 `res/values-en`。
