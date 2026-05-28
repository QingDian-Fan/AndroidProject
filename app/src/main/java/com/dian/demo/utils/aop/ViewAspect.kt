package com.dian.demo.utils.aop

import android.util.Log
import com.project.common.utils.FastClickUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

@Aspect
class ViewAspect {

    @Around("execution(* android.view.View.OnClickListener.onClick(android.view.View))")
    fun callOnClick(joinPoint: ProceedingJoinPoint) {
        Log.e("TAG--->View", "-----callOnClick-----")
        if (!FastClickUtil.isFastClick()) {
            // 不是快速点击，执行原方法
            Log.e("TAG--->View", "-----callOnClick do it-----")
            joinPoint.proceed()
        }
    }


}