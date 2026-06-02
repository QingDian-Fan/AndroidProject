# lib_annotation

`lib_annotation` 是登录流程 APT 的注解定义模块，类型为 Java Library。它只提供编译期注解，不包含 Android 运行时依赖。

## 模块定位

- 定义登录流程中需要被注解处理器扫描的注解。
- 与 `lib_processor` 配套使用，为应用生成集中式登录 Hook 辅助代码。

## 注解说明

- `@RequireLogin`: 标记进入前需要登录的页面。
- `@LoginPage`: 标记登录页面。
- `@CheckLogin`: 标记用于判断当前登录态的方法。

## 使用方式

在应用模块中添加：

```groovy
dependencies {
    implementation project(':lib_annotation')
    kapt project(':lib_processor')
}
```

然后在页面和登录态判断方法上添加对应注解。

## 注意事项

- `@CheckLogin` 标记的方法需要能被生成代码静态调用。
- 该模块应保持轻量，不引入 Android 或业务依赖。
