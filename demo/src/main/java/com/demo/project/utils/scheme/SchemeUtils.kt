package com.demo.project.utils.scheme

import android.content.Context
import android.net.Uri
import android.util.Log
import java.lang.reflect.Method

object SchemeUtils {

    private const val TAG = "SchemeUtils"

    fun toOpenActivity(mContext: Context, urlString: Uri) {
        when (urlString.host) {
            KEY_SPLASH_ACTIVITY -> navigateActivity(mContext, PATH_SPLASH_ACTIVITY)
            KEY_HOME_ACTIVITY -> navigateActivity(mContext, PATH_HOME_ACTIVITY)
            KEY_LOGIN_ACTIVITY -> navigateActivity(mContext, PATH_LOGIN_ACTIVITY)
            KEY_WEB_ACTIVITY -> navigateActivity(
                mContext,
                PATH_WEB_ACTIVITY,
                urlString.getQueryParameter("link_url"),
                urlString.getQueryParameter("title")
            )
            KEY_BROWSER_ACTIVITY -> navigateActivity(
                mContext,
                PATH_BROWSER_ACTIVITY,
                urlString.getQueryParameter("link_url")
            )
            KEY_CAMERA_ACTIVITY -> navigateActivity(mContext, PATH_CAMERA_ACTIVITY)
            KEY_VIDEO_ACTIVITY -> navigateActivity(
                mContext,
                PATH_VIDEO_ACTIVITY,
                urlString.getQueryParameter("url")
            )
            KEY_DEBUG_ACTIVITY -> navigateActivity(mContext, PATH_DEBUG_ACTIVITY)
            else -> Log.w(TAG, "未匹配到 host: ${urlString.host}")
        }
    }

    /**
     * 通过反射调用目标 Activity 伴生对象中的 `start` 方法。
     *
     * 各 Activity 的 `start` 方法签名不同（参数个数不一），这里会从所有名为
     * `start` 的方法里挑选参数个数完全匹配的一个进行调用。传入的 [params] 会按原
     * 值、原顺序转发给目标方法，不做裁剪或重组。
     *
     * @param context 启动上下文
     * @param path    目标 Activity 的全限定类名
     * @param params  传给 start 方法的可变参数（Context 之后的参数）
     */
    fun navigateActivity(context: Context, path: String, vararg params: String?) {
        try {
            val clazz = Class.forName(path)

            // 候选方法：既包含宿主类的 @JvmStatic 静态方法，也包含伴生对象的实例方法
            val companion = runCatching { clazz.getField("Companion").get(null) }.getOrNull()
            val candidates = buildList {
                clazz.declaredMethods.forEach { add(it to null) }
                companion?.let { c -> c.javaClass.declaredMethods.forEach { add(it to c) } }
            }

            val required = params.size + 1 // 算上 Context

            candidates.firstOrNull { (m, _) ->
                m.name == "start" && m.parameterTypes.size == required && firstIsContext(m)
            }?.let { (m, target) ->
                m.isAccessible = true
                val args = arrayOfNulls<Any?>(required)
                args[0] = context
                for (i in 1 until required) args[i] = params[i - 1]
                m.invoke(target, *args)
                return
            }

            Log.w(TAG, "$path 未找到匹配的 start 方法, 参数个数=${params.size}")
        } catch (e: Exception) {
            Log.e(TAG, "navigateActivity 调用失败: $path", e)
        }
    }

    /** 判断方法的第一个参数是否为 Context（或其子类）。 */
    private fun firstIsContext(method: Method): Boolean {
        val types = method.parameterTypes
        return types.isNotEmpty() && Context::class.java.isAssignableFrom(types[0])
    }
}
