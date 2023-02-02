package com.dian.demo.utils.ext

import android.view.View
import com.dian.demo.utils.LogUtil

/**
 * @author: Albert Li
 * @contact: albertlii@163.com
 * @time: 2020/6/7 5:32 PM
 * @description: View相关扩展
 * @since: 1.0.0
 */

fun <T : View> T.click(action: (T) -> Unit) {
    setOnClickListener {
        action(this)
    }
}

fun <T : View> T.longClick(action: (T) -> Boolean) {
    setOnLongClickListener {
        action(this)
    }
}

/**
 * 带有限制快速点击的点击事件
 */
fun <T : View> T.singleClick(interval: Long = 800L, action: ((T) -> Unit)?) {
    setOnClickListener(SingleClickListener(interval, action))
}

private var lastClickTime = 0L
private lateinit var mView: View

class SingleClickListener<T : View>(
    private val interval: Long = 800L,
    private var clickFunc: ((T) -> Unit)?
) : View.OnClickListener {

    override fun onClick(v: View) {
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastClickTime > interval || mView != v) {
            LogUtil.e("当前时间(${nowTime})-上次时间(${lastClickTime}) = 间隔时间(${nowTime - lastClickTime})")
            // 单次点击事件
            clickFunc?.invoke(v as T)
            lastClickTime = nowTime
            mView = v
        }
    }
}