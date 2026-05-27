# lib_common-aop

`lib_common-aop` 是公共 AOP 模块，类型为 Android Library，命名空间为 `com.common.aop`。它基于 AspectJ 提供切面能力。

## 模块定位

- 使用注解和切面统一处理横切逻辑。
- 减少页面中的重复判断代码，例如防重复点击、网络检查、权限检查等。

## 主要能力

- `@SingleClick`: 防重复点击。
- `@CheckNet`: 网络可用性检查。
- `@CheckPermissions`: 权限检查。
- `SingleClickAspect`: 防重复点击切面。
- `CheckNetAspect`: 网络检查切面。
- `CheckPermissionsAspect`: 权限检查切面。
- `AndroidAspect`、`ViewAspect`: Android/View 相关切面扩展。

## 依赖关系

- 依赖 `lib_common-utils`。
- 使用 `org.aspectj:aspectjrt`，并在 Gradle 中通过 AspectJ tools 执行 weave。

## 使用建议

- 切面逻辑要保持轻量，避免在切面中直接写复杂业务。
- 注解命名应表达行为意图，调用方不需要了解底层 weave 细节。
- 修改切面后建议运行应用模块 assemble，确认 weave 过程和运行时代码都正常。
