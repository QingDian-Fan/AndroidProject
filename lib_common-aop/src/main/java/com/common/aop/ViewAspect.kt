package com.common.aop

import android.view.View
import com.common.utils.FastClickUtil
import com.common.utils.LogUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

@Aspect
class ViewAspect {

    @Around("call(void android.view.View+.setOnClickListener(android.view.View.OnClickListener)) && args(listener)")
    fun callOnClick(joinPoint: ProceedingJoinPoint, listener: View.OnClickListener?) {
        if (listener == null) {
            joinPoint.proceed()
            return
        }

        val debounceListener = View.OnClickListener { view ->
            LogUtil.e("TAG--->AOP", "-----ViewAspect-Before-----")
            if (!FastClickUtil.isFastClick()) {
                LogUtil.e("TAG--->AOP", "-----ViewAspect-Inner-----")
                listener.onClick(view)
            }
        }
        joinPoint.proceed(arrayOf(debounceListener))
    }


}
