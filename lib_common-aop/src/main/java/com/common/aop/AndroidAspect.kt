package com.common.aop

import android.app.Activity
import com.common.utils.LogUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect


@Aspect
class AndroidAspect {

    @Around(
        "target(activity) && (" +
            "execution(* android.app.Activity+.onCreate(..)) || " +
            "execution(* android.app.Activity+.onStart(..)) || " +
            "execution(* android.app.Activity+.onResume(..)) || " +
            "execution(* android.app.Activity+.onPause(..)) || " +
            "execution(* android.app.Activity+.onStop(..)) || " +
            "execution(* android.app.Activity+.onRestart(..)) || " +
            "execution(* android.app.Activity+.onDestroy(..)) || " +
            "execution(* android.app.Activity+.onSaveInstanceState(..)) || " +
            "execution(* android.app.Activity+.onRestoreInstanceState(..))" +
            ")"
    )
    fun onActivityCalled(joinPoint: ProceedingJoinPoint, activity: Activity): Any? {
        LogUtil.e(
            "TAG--->AOP",
            "ActivityLifecycle--->${activity.javaClass.name}::${joinPoint.signature.name}"
        )
        return joinPoint.proceed()
    }

}
