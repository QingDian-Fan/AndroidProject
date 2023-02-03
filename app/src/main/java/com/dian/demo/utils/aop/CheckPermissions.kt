package com.dian.demo.utils.aop




@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
annotation class CheckPermissions constructor(
    /**
     * 需要申请权限的集合
     */
    vararg  val value: String
)
