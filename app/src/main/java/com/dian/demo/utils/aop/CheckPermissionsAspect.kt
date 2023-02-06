package com.dian.demo.utils.aop

import android.app.Activity
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dian.demo.ui.activity.DemoActivity
import com.dian.demo.ui.dialog.ConfirmDialog
import com.dian.demo.ui.dialog.TipDialog
import com.dian.demo.utils.ActivityManager
import com.dian.demo.utils.LogUtil
import com.dian.demo.utils.ext.showAllowStateLoss
import com.dian.demo.utils.permissions.LivePermissions
import com.dian.demo.utils.permissions.PermissionResult
import com.dian.demo.utils.permissions.PermissionsUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature

/**

 */
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
        val isMustPermission = checkPermissions?.isMust
        LogUtil.e("TAGTAG", "AOP-CheckPermissions-do it")
        if (PermissionsUtil.hasAopPermission(mActivity, permissionList)) {
            joinPoint.proceed()
        } else {
            requestPermissions(joinPoint, mActivity, permissionList,isMustPermission?:false)
        }
    }

    private fun requestPermissions(
        joinPoint: ProceedingJoinPoint,
        mActivity: AppCompatActivity,
        permissions: Array<out String>,
        isMustPermission:Boolean
    ) {
        LivePermissions(mActivity).requestArray(permissions)
            .observe(mActivity) {
                when (it) {
                    is PermissionResult.Grant -> {  //权限允许
                        LogUtil.e("TAGTAG", "AOP-CheckPermissions-权限允许")
                    }
                    is PermissionResult.Rationale -> {  //权限拒绝
                        LogUtil.e("TAGTAG", "AOP-CheckPermissions-权限拒绝")
                        if (isMustPermission){
                                val dialog= TipDialog.getDialog("提示","请到设置中打开权限，否则无法使用该功能")
                                dialog.showAllowStateLoss(mActivity.supportFragmentManager, "")
                            } else {
                                joinPoint.proceed()
                            }

                    }
                    is PermissionResult.Deny -> {   //权限拒绝，且勾选了不再询问
                        LogUtil.e("TAGTAG", "AOP-CheckPermissions-权限拒绝，且勾选了不再询问")
                        if (isMustPermission){
                            val dialog= ConfirmDialog.getDialog("提示","请到设置中打开权限，否则无法使用该功能","dian://setting")
                            dialog.showAllowStateLoss(mActivity.supportFragmentManager, "")
                        } else {
                            joinPoint.proceed()
                        }
                    }
                }
            }
    }
}
