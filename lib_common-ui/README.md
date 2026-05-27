# lib_common-ui

`lib_common-ui` 是公共 UI 基础模块，类型为 Android Library，命名空间为 `com.common.ui`。它提供 Activity、Fragment、ViewModel、DataBinding 页面容器和页面状态管理等基础能力。

## 模块定位

- 统一页面基类和 MVVM 使用方式。
- 提供页面加载、空页、错误页、Toast、导航、返回和关闭页面等行为抽象。
- 复用 `lib_common-utils`、`lib_common-theme`、`lib_common-weight` 中的基础能力。

## 主要能力

- `BaseActivity`、`BaseFragment`: 基础生命周期、Toast、导航和懒加载。
- `BaseAppBindActivity`、`BaseAppBindFragment`: DataBinding 页面封装。
- `BaseAppVMActivity`、`BaseAppVMFragment`: ViewModel 注入和内部事件观察。
- `BaseViewModel`: 页面事件、加载态、空态、错误态、导航和返回事件。
- `ViewBehavior`、`ILazyLoad`: 页面行为和懒加载协议。
- `skin/*`: 换肤、语言和主题偏好相关基础类。

## 依赖关系

- 依赖 `lib_common-utils`
- 依赖 `lib_common-theme`
- 依赖 `lib_common-weight`

## 使用建议

- 新页面优先继承本模块基类，统一生命周期和状态处理。
- ViewModel 与页面通信优先使用基类提供的事件通道，减少 Activity/Fragment 之间的强耦合。
- 页面状态布局资源建议在本模块统一维护，业务模块只关注内容区域。
