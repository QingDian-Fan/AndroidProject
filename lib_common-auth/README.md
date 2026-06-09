# lib_common-auth

通用登录态与登录拦截模块，负责把登录状态存储、登录页跳转、登录成功回跳、APT 登录注解和 AMS Hook 放到一个独立能力库中。

## 接入方式

1. App 模块依赖 `lib_common-auth`，并继续使用 `lib_annotation` + `lib_processor` 生成登录路由信息。
2. 在 `Application.onCreate()` 中初始化：

```kotlin
AuthManager.init()
LoginHookUtil.HookAms(this)
```

3. 登录页标记：

```kotlin
@LoginPage
class LoginActivity : BaseSkinBindActivity<ActivityLoginBinding>()
```

4. 登录判断方法标记：

```kotlin
object LoginState {
    @JvmStatic
    @CheckLogin
    fun isLogin(): Boolean = AuthManager.isLogin()
}
```

5. 登录成功后保存状态并回跳原页面：

```kotlin
AuthManager.completeLogin(
    activity,
    AuthSession(token = token, userId = userId, userName = userName)
)
```

## 可扩展点

- `AuthStore`：可替换默认 DataStore 存储。
- `AuthConfig.loginActivity`：可手动指定登录页，绕过 APT 反射。
- `AuthConfig.loginIntentFactory`：可自定义登录 Intent。
- `AuthConfig.loginChecker`：可接入宿主已有登录态判断。
- `AuthTokenProvider`：可给网络层读取 token header。
