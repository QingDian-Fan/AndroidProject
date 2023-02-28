package com.dian.demo.utils.aop

import android.util.Log
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.ProceedingJoinPoint


@Aspect
class AndroidAspect {


    @Around("execution(* android.app.Activity.on**(..))")
    fun onActivityCalled( joinPoint: ProceedingJoinPoint) {
        val signature = joinPoint.signature
        Log.e("TAGATAG-AOP","Method--->:${ signature.name}")
        joinPoint.proceed()
    }

}