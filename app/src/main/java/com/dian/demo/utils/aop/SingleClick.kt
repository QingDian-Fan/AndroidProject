package com.dian.demo.utils.aop

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER)
annotation class SingleClick constructor(
    /**
     * 快速点击的间隔
     */
    val value: Long = 800
)