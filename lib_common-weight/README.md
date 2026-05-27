# lib_common-weight

`lib_common-weight` 是公共控件模块，类型为 Android Library，命名空间为 `com.common.weight`。

## 模块定位

- 沉淀项目中可复用的自定义控件。
- 为 `lib_common-ui` 等上层模块提供基础控件支持。

## 主要内容

- `CommonTitleBar`: 通用标题栏控件。
- `StatusBarUtils`: 状态栏处理工具。
- `FlymeStatusBarUtils`: Flyme 系统状态栏适配。
- `KeyboardConflictCompat`: 键盘冲突处理。
- `ScreenUtils`、`OSUtils`: 屏幕和系统判断工具。

## 依赖关系

该模块不依赖其他项目内模块，只依赖 AndroidX、AppCompat 和 Material 等基础库。

## 使用建议

- 只放真正可跨业务复用的 View 或控件辅助类。
- 控件资源命名建议加公共前缀，降低与 app 资源冲突的概率。
