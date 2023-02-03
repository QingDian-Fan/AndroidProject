package com.dian.demo.utils.aop

import android.util.Log
import android.view.View
import com.dian.demo.utils.FastClickUtil.isFastClick
import com.dian.demo.utils.LogUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature


/**
 * 如果这个文件是 java文件则只有java代码会生效
 * 如果是kotlin文件则java和kotlin代码都会生效
 */
@Aspect
class SingleClickAspect {
    /**
     * 定义切点，标记切点为所有被@SingleClick注解的方法
     * 自己项目中SingleClick这个类的全路径哦
     */
    @Pointcut("execution(@com.dian.demo.utils.aop.SingleClick * *(..))")
    fun methodAnnotated() {
    }

    /**
     * 定义一个切面方法，包裹切点方法
     */
    @Around("methodAnnotated()")
    @Throws(Throwable::class)
    fun aroundJoinPoint(joinPoint: ProceedingJoinPoint) {
        LogUtil.e("TAGTAG", "AOP-SingleClick")
        // 取出方法的参数
        var view: View? = null
        for (arg in joinPoint.args) {
            if (arg is View) {
                view = arg
                break
            }
        }
        if (view == null) {
            return
        }
        // 取出方法的注解
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        if (method == null || !method.isAnnotationPresent(SingleClick::class.java)) {
            return
        }
        val singleClick = method.getAnnotation(SingleClick::class.java)
        // 判断是否快速点击
        if (!isFastClick()) {
            LogUtil.e("TAGTAG", "AOP-SingleClick-do it")
            // 不是快速点击，执行原方法
            joinPoint.proceed()
        }
    }
}