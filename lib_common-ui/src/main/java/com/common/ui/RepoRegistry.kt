package com.common.ui

/**
 * @description: Repository 注册表。实现类位于 app 模块（如 demo），通过编译期注解处理器
 * 生成的 com.dian.generated.RepoModule.init() 在 App 启动时注册进来；BaseViewModel 仅
 * 依赖本注册表读取，从而把具体实现的依赖方向反转，无需反射、也无需 lib 依赖 app。
 */
object RepoRegistry {

    /** Java 友好的工厂接口，便于生成的 Java 代码用方法引用（如 DataRepoImpl::new）注册。 */
    fun interface Provider<T> {
        fun create(): T
    }

    @PublishedApi
    internal val providers = HashMap<Class<*>, Provider<*>>()

    @JvmStatic
    fun <T : Any> register(clazz: Class<T>, provider: Provider<T>) {
        providers[clazz] = provider
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(clazz: Class<T>): T {
        val provider = providers[clazz]
            ?: throw IllegalStateException(
                "No repo registered for ${clazz.name}. " +
                    "请确认实现类已标注 @Repo，且 App 启动时调用了 RepoModule.init()。"
            )
        return (provider as Provider<T>).create()
    }
}