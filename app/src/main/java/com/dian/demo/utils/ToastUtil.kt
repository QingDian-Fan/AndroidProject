package com.dian.demo.utils

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.dian.demo.ProjectApplication

/**
 * @author:  QingDian_Fan
 * @contact: dian.work@foxmail.com
 * @time: 2021/8/16 2:52 下午
 * @description: Toast相关工具类
 * @since: 1.0.0
 */
object ToastUtil {
    var toast: Toast? = null

    @JvmStatic
    fun showToast(
        context: Context = ProjectApplication.getAppContext(),
        str: String,
        showLong: Boolean = false,
        gravity: Int = Gravity.CENTER
    ) {
        val tempToast = Toast.makeText(
            context.applicationContext, str,
            if (showLong) {
                Toast.LENGTH_LONG
            } else {
                Toast.LENGTH_SHORT
            }
        )
        tempToast?.setGravity(gravity, 0, 0)
        tempToast?.show()

    }

    fun showViewToast(
        context: Context,
        view: View,
        showLong: Boolean,
        gravity: Int = Gravity.CENTER
    ) {
        if (toast == null) {
            toast = Toast(context.applicationContext)
        }
        toast?.setGravity(gravity, 0, 0)
        toast?.view = view
        toast?.duration = if (showLong) {
            Toast.LENGTH_LONG
        } else {
            Toast.LENGTH_SHORT
        }
        cancelToast(toast)
        toast?.show()
    }

    fun cancelToast(toast: Toast?) {
        try {
            if (toast?.view!!.isShown) {
                toast.cancel()
            }
        } catch (e: Exception) {
        }
    }
}