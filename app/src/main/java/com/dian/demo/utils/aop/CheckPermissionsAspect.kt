package com.dian.demo.utils.aop

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dian.demo.ui.activity.DemoActivity
import com.dian.demo.utils.ActivityManager
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.permissions.LivePermissions
import com.dian.demo.utils.permissions.PermissionResult
import com.dian.demo.utils.permissions.PermissionsUtil
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature

@Aspect
class CheckPermissionsAspect {

    @Pointcut("execution(@com.dian.demo.utils.aop.CheckPermissions * *(..))")
    fun methodPermissions() {

    }

    @Around("methodPermissions()")
    @Throws(Throwable::class)
    fun aroundJoinPermissions(joinPoint: ProceedingJoinPoint) {
        LogUtil.e("TAGTAG", "AOP-CheckPermissions")
        var mActivity: AppCompatActivity? = null
        for (arg in joinPoint.args) {
            if (arg is AppCompatActivity) {
                mActivity = arg
                break
            }
        }
        if ((mActivity == null) || mActivity.isFinishing || mActivity.isDestroyed) {
            LogUtil.e("TAGTAG", "AOP-CheckPermissions-mActivity is null")
            mActivity = ActivityManager.getInstance().getTopActivity() as AppCompatActivity
        }
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        if (method === null || !method.isAnnotationPresent(CheckPermissions::class.java)) {
            return
        }
        val checkPermissions = method.getAnnotation(CheckPermissions::class.java)
        val permissionList: Array<out String> = checkPermissions?.value ?: arrayOf()
        LogUtil.e("TAGTAG", "AOP-CheckPermissions-do it")
        if (PermissionsUtil.hasAopPermission(mActivity, permissionList)) {
            joinPoint.proceed()
        } else {
            requestPermissions(joinPoint, mActivity, permissionList)
        }
    }

    private fun requestPermissions(
        joinPoint: ProceedingJoinPoint,
        mActivity: AppCompatActivity,
        permissions: Array<out String>
    ) {
        LivePermissions(mActivity).requestArray(permissions)
            .observe(mActivity) {
                when (it) {
                    is PermissionResult.Grant -> {  //权限允许
                        LogUtil.e("TAGTAG", "AOP-CheckPermissions-权限允许")
                    }
                    is PermissionResult.Rationale -> {  //权限拒绝
                        LogUtil.e("TAGTAG", "AOP-CheckPermissions-权限拒绝")
                    }
                    is PermissionResult.Deny -> {   //权限拒绝，且勾选了不再询问
                        LogUtil.e("TAGTAG", "AOP-CheckPermissions-权限拒绝，且勾选了不再询问")
                    }
                }
                joinPoint.proceed()
            }
    }
}
