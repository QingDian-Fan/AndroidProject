package com.common.aop

import com.common.utils.LogUtil
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
        LogUtil.e("TAG--->AOP", "-----CheckNetAspect-Before-----")
        if (NetWorkUtil.isNetworkAvailable()){
            LogUtil.e("TAG--->AOP", "CheckNetAspect-isNetworkAvailable::true")
            joinPoint.proceed()
        }else{
            LogUtil.e("TAG--->AOP", "CheckNetAspect-isNetworkAvailable::false-请检查您的网络")
            ToastUtil.showToast(str = "请检查您的网络")
        }
    }
}
