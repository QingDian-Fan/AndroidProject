#############################################
# lib_common-http consumer rules（Retrofit / Room / Moshi）
#############################################

# Retrofit 依据方法泛型返回值与 suspend 续体签名构造调用；Moshi 反射适配器依据 Kotlin 元数据建模。
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,AnnotationDefault
# moshi-kotlin 的 KotlinJsonAdapter 读取 @Metadata 还原构造参数名与可空性。
-keep class kotlin.Metadata { *; }

#--------------------------------------------
# Retrofit
#--------------------------------------------
# Retrofit 通过动态代理实现接口，需要方法上的 @GET/@POST 等注解与泛型签名。
# 依据：RequestService.kt 及使用方自定义的 API 接口。允许接口名混淆，只保留方法与注解。
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

#--------------------------------------------
# JSON 模型（Moshi 反射适配器）
#--------------------------------------------
# 依据：HttpClient.kt 中 MoshiUtil.fromJson<Result<T>>(body, type)，
# Result 由反射适配器按字段名解析，字段名与主构造函数不能被裁剪或改名。
-keep class com.common.http.Result { *; }
# HttpError 作为错误信息载体在宿主侧读取字段，保留成员名即可，类名允许混淆。
-keepclassmembers class com.common.http.HttpError { *; }

#--------------------------------------------
# Cookie 持久化（Java 序列化）
#--------------------------------------------
# 依据：PersistentCookieStore.encodeCookie() / decodeCookie() 用 ObjectOutputStream
# 序列化 OkHttpCookies。okhttp3.Cookie 本身不实现 Serializable，因此 OkHttpCookies
# 依赖私有 writeObject()/readObject() 钩子手工读写字段——这两个方法只被 JDK 序列化机制
# 反射调用，R8 视为不可达并裁剪，之后会退化为默认序列化并抛 NotSerializableException，
# 异常被吞掉后 Cookie 无法落盘，进程重启即丢失登录态。
# 同时保留类名可稳定跨版本的默认 serialVersionUID，避免历史持久化数据反序列化失败。
-keep class com.common.http.cookie.OkHttpCookies { *; }

#--------------------------------------------
# Room
#--------------------------------------------
# Room 在运行期用 "数据库类名 + _Impl" 反射实例化生成实现类，
# 因此数据库类名与生成实现类必须保持一致。依据：room/AppDatabase.kt
-keepnames class com.common.http.room.AppDatabase
-keep class com.common.http.room.AppDatabase_Impl { *; }
# Entity 字段由 Room 生成代码按列名绑定，保留字段与构造函数。
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
    <init>(...);
}

#--------------------------------------------
# 可选依赖告警
#--------------------------------------------
# OkHttp 编译期引用的可选 TLS Provider，Android 运行时不提供这些实现。
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# OkHttp/Moshi 引用的 JSR-305 注解，仅编译期可见。
-dontwarn javax.annotation.**
# Room 可选的 paging 集成，本模块未引入 androidx.paging。
-dontwarn androidx.room.paging.**

# 说明：APIConfig.toLoginActivity() 反射的 "com.example.app.LoginActivity$Companion"
# 在本项目中不存在（示例代码，调用即抛异常并被上层 try/catch 吞掉），
# 属于无真实运行路径的反射，按最小保留原则不为其添加 keep 规则。
