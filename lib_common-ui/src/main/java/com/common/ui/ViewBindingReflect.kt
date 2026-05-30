package com.common.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import java.lang.reflect.ParameterizedType

/**
 * ViewBinding 反射辅助：子类只需通过 getLayoutId() 提供布局 id，
 * 这里负责 inflate 布局并调用对应 ViewBinding 生成类的静态 `bind(View)` 完成绑定。
 */
internal object ViewBindingReflect {

    /**
     * inflate [layoutId] 对应的布局（attachToParent = false），并通过 [host] 泛型继承链上
     * 解析出的 ViewBinding 类型调用其 `bind(View)` 返回绑定对象。
     *
     * @param host   持有 ViewBinding 泛型参数的对象（Activity / Fragment 实例）
     * @param parent 仅用于 inflate 时计算 LayoutParams，不会自动 addView
     */
    @Suppress("UNCHECKED_CAST")
    fun <B : ViewBinding> bind(
        host: Any,
        inflater: LayoutInflater,
        parent: ViewGroup?,
        @LayoutRes layoutId: Int,
    ): B {
        require(layoutId != 0) {
            "${host.javaClass.simpleName} 必须重写 getLayoutId() 返回有效的布局 id"
        }
        val view = inflater.inflate(layoutId, parent, false)
        val bindingClass = findBindingClass(host.javaClass)
        val bindMethod = bindingClass.getMethod("bind", View::class.java)
        return bindMethod.invoke(null, view) as B
    }

    /** 沿继承链向上查找第一个 ViewBinding 子类型的泛型实参 */
    private fun findBindingClass(start: Class<*>): Class<*> {
        var clazz: Class<*>? = start
        while (clazz != null) {
            val superType = clazz.genericSuperclass
            if (superType is ParameterizedType) {
                superType.actualTypeArguments.forEach { arg ->
                    if (arg is Class<*> && ViewBinding::class.java.isAssignableFrom(arg)) {
                        return arg
                    }
                }
            }
            clazz = clazz.superclass
        }
        throw IllegalStateException("未在 ${start.name} 的继承链上找到 ViewBinding 泛型参数")
    }
}
