package com.common.aop

import com.common.utils.FastClickUtil
import com.common.utils.LogUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

@Aspect
class ViewAspect {

    @Around("execution(* android.view.View.OnClickListener.onClick(android.view.View))")
    fun callOnClick(joinPoint: ProceedingJoinPoint) {
        LogUtil.e("TAG--->View", "-----callOnClick-----")
        if (!FastClickUtil.isFastClick()) {
            // 不是快速点击，执行原方法
            LogUtil.e("TAG--->View", "-----callOnClick do it-----")
            joinPoint.proceed()
        }
    }


}