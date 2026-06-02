package com.common.aop


import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.common.utils.ActivityManager
import com.common.utils.LogUtil
import com.common.utils.ToastUtil
import com.common.utils.permissions.DefaultPermissionInterceptor
import com.common.utils.permissions.LivePermissions
import com.common.utils.permissions.PermissionResult
import com.common.utils.permissions.PermissionsUtil
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature

/**

 */
@Aspect
class CheckPermissionsAspect {

    @Pointcut("execution(@com.common.aop.CheckPermissions * *(..))")
    fun methodPermissions() {

    }

    private val mHandler = Handler(Looper.getMainLooper())

    @Around("methodPermissions()")
    @Throws(Throwable::class)
    fun aroundJoinPermissions(joinPoint: ProceedingJoinPoint) {
        LogUtil.e("TAG--->AOP", "-----CheckPermissionsAspect-Before-----")

        var mActivity: AppCompatActivity? = null
        for (arg in joinPoint.args) {
            if (arg is AppCompatActivity) {
                mActivity = arg
                break
            }
        }
        if ((mActivity == null) || mActivity.isFinishing || mActivity.isDestroyed) {
            LogUtil.e("TAG--->AOP", "CheckPermissionsAspect::AOP-CheckPermissions-mActivity is null")

            mActivity = ActivityManager.getInstance().getTopActivity() as? AppCompatActivity
        }
        if (mActivity == null || mActivity.isFinishing || mActivity.isDestroyed) {
            return
        }
        val methodSignature = joinPoint.signature as MethodSignature
        val method = methodSignature.method
        if (method === null || !method.isAnnotationPresent(CheckPermissions::class.java)) {
            return
        }
        val checkPermissions = method.getAnnotation(CheckPermissions::class.java)
        val permissionList: Array<out String> = checkPermissions?.value ?: arrayOf()
        val isMustPermission = checkPermissions?.isMust
        LogUtil.e("TAG--->AOP", "-----CheckPermissionsAspect-do request-----")
        if (PermissionsUtil.hasAopPermission(mActivity, permissionList)) {
            joinPoint.proceed()
        } else {
            requestPermissions(joinPoint, mActivity, permissionList, isMustPermission ?: false)
        }
    }

    private fun requestPermissions(
        joinPoint: ProceedingJoinPoint,
        mActivity: AppCompatActivity,
        permissions: Array<out String>,
        isMustPermission: Boolean
    ) {
        mHandler.post {
            LivePermissions.getInstance(mActivity).addInterceptor(DefaultPermissionInterceptor())
                .requestArray(permissions)
                .observe(mActivity) {
                    when (it) {
                        is PermissionResult.Grant -> {  //权限允许
                            joinPoint.proceed()
                        }

                        is PermissionResult.Rationale -> {  //权限拒绝
                            if (isMustPermission) {
                                ToastUtil.showToast( str="请到设置中打开权限，否则无法使用该功能")
                            } else {
                                joinPoint.proceed()
                            }
                        }

                        is PermissionResult.Deny -> {   //权限拒绝，且勾选了不再询问
                            if (isMustPermission) {
                                ToastUtil.showToast( str="请到设置中打开权限，否则无法使用该功能")
                            } else {
                                joinPoint.proceed()
                            }
                        }
                    }
                }
        }

    }
}
