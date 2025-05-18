package com.dian.demo.utils.aop

import android.util.Log
import com.dian.demo.ProjectApplication
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.NetWorkUtil
import com.dian.demo.utils.ToastUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut

@Aspect
class CheckNetAspect {


    @Pointcut("execution(@com.dian.demo.utils.aop.CheckNet * *(**))")
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