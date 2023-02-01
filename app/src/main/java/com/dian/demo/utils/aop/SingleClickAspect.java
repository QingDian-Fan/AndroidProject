package com.dian.demo.utils.aop;

import android.util.Log;
import android.view.View;

import com.dian.demo.utils.FastClickUtil;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 实现防止按钮连续点击
 * @author jiang zhu on 2019/4/19
 */
@Aspect
public class SingleClickAspect {

    @Before("execution(* android.app.Activity.**(..))")
    public void onActivityMethodBefore(JoinPoint joinPoint) throws Throwable {
        String key = joinPoint.getSignature().toString();
        Log.e("TAGTAG","AOP-onActivityMethodBefore"+key);
    }

    /**
     * 定义切点，标记切点为所有被@SingleClick注解的方法
     * 自己项目中SingleClick这个类的全路径哦
     */
    @Pointcut("execution(@com.dian.demo.utils.aop.SingleClick * *(..))")
    public void methodAnnotated() {
    }

    /**
     * 定义一个切面方法，包裹切点方法
     */
    @Around("methodAnnotated()")
    public void aroundJoinPoint(ProceedingJoinPoint joinPoint) throws Throwable {
        Log.e("TAGTAG","AOP-aroundJoinPoint");
        // 取出方法的参数
        View view = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof View) {
                view = (View) arg;
                break;
            }
        }
        if (view == null) {
            return;
        }
        // 取出方法的注解
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        if (!method.isAnnotationPresent(SingleClick.class)) {
            return;
        }
        SingleClick singleClick = method.getAnnotation(SingleClick.class);
        // 判断是否快速点击
        if (!FastClickUtil.INSTANCE.isFastClick()) {
            // 不是快速点击，执行原方法
            joinPoint.proceed();
        }
    }
}