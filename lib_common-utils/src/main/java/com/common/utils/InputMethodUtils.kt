package com.common.utils

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * 描述：弹出软件盘辅助类
 *
 * @author Yanbo
 * @date 18/9/1
 */
object InputMethodUtils {

    /**
     * 禁止EditText弹出软件盘，光标依然正常显示。
     */
    @JvmStatic
    fun disableShowSoftInput(editText: EditText) {
        val cls = EditText::class.java
        try {
            val method = cls.getMethod("setShowSoftInputOnFocus", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(editText, false)
        } catch (ignore: Exception) {
        }
        try {
            val method = cls.getMethod("setSoftInputShownOnFocus", Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            method.invoke(editText, false)
        } catch (ignore: Exception) {
        }
    }

    /**
     * 隐藏虚拟键盘
     */
    @JvmStatic
    fun hide(v: View) {
        val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(v.applicationWindowToken, 0)
    }

    /**
     * 显示虚拟键盘
     */
    @JvmStatic
    fun show(v: View) {
        val imm = v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(v, 0)
    }
}