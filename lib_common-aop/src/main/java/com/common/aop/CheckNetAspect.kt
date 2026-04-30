package com.common.aop

import com.common.utils.NetWorkUtil
import com.common.utils.ToastUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut

@Aspect
class CheckNetAspect {


    @Pointcut("execution(@com.common.aop.CheckNet * *(**))")
    fun methodCheckNet(){

    }

    @Around("methodCheckNet()")
    fun aroundMethodCheckNet(joinPoint: ProceedingJoinPoint){
        if (NetWorkUtil.isNetworkAvailable()){
            joinPoint.proceed()
        }else{
            ToastUtil.showToast(str = "请检查您的网络")
        }
    }
}
