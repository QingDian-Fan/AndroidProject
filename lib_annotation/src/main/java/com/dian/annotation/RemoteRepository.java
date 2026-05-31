package com.dian.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个 Repository 实现类，编译期由 RepoProcessor 收集并生成
 * {@code com.dian.generated.RepoModule}，在 App 启动时把实现注册进
 * {@code com.common.ui.RepoRegistry}，供 BaseViewModel 的 repo() 取用。
 *
 * <p>value 指定要绑定的接口（即 BaseViewModel 中 repo&lt;T&gt;() 使用的 T）。
 * 不填则默认绑定该类直接实现的全部接口，若没有接口则绑定到类自身。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface RemoteRepository {
    Class<?>[] value() default {};
}
