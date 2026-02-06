package com.dian.demo.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.collection.ArrayMap
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.dian.demo.DataProtoOuterClass
import kotlin.system.exitProcess


class ActivityManager private constructor() : ActivityLifecycleCallbacks {

    companion object {

        @Suppress("StaticFieldLeak")
        private val activityManager: ActivityManager by lazy { ActivityManager() }

        fun getInstance(): ActivityManager {
            return activityManager
        }

        /**
         * 获取一个对象的独立无二的标记
         */
        private fun getObjectTag(mObject: Any): String {
            // 对象所在的包名 + 对象的内存地址
            return mObject.javaClass.name + Integer.toHexString(mObject.hashCode())
        }
    }

    /** Activity 存放集合 */
    private val activitySet: ArrayMap<String?, Activity?> = ArrayMap()

    /** 应用生命周期回调 */
    private val lifecycleCallbacks: ArrayList<ApplicationLifecycleCallback> = ArrayList()

    /** 当前应用上下文对象 */
    private lateinit var application: Application

    /** 栈顶的 Activity 对象 */
    private var topActivity: Activity? = null

    /** 前台并且可见的 Activity 对象 */
    private var resumedActivity: Activity? = null

    fun init(application: Application) {
        this.application = application
        this.application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * 获取 Application 对象
     */
    fun getApplication(): Application {
        return application
    }

    /**
     * 获取栈顶的 Activity
     */
    fun getTopActivity(): Activity? {
        return topActivity
    }

    /**
     * 获取前台并且可见的 Activity
     */
    fun getResumedActivity(): Activity? {
        return resumedActivity
    }

    /**
     * 判断当前应用是否处于前台状态
     */
    fun isForeground(): Boolean {
        return getResumedActivity() != null
    }

    /**
     * 注册应用生命周期回调
     */
    fun registerApplicationLifecycleCallback(callback: ApplicationLifecycleCallback) {
        lifecycleCallbacks.add(callback)
    }

    /**
     * 取消注册应用生命周期回调
     */
    fun unregisterApplicationLifecycleCallback(callback: ApplicationLifecycleCallback) {
        lifecycleCallbacks.remove(callback)
    }

    /**
     * 销毁指定的 Activity
     */
    fun finishActivity(clazz: Class<out Activity?>?) {
        if (clazz == null) {
            return
        }
        val keys: Array<String?> = activitySet.keys.toTypedArray()
        for (key: String? in keys) {
            val activity: Activity? = activitySet[key]
            if (activity == null || activity.isFinishing) {
                continue
            }
            if ((activity.javaClass == clazz)) {
                activity.finish()
                activitySet.remove(key)
                break
            }
        }
    }

    /**
     * 销毁所有的 Activity
     */
    fun finishAllActivities() {
        finishAllActivities(null as Class<out Activity?>?)
    }

    fun restartAPP(context: Context) {

        val data = DataProtoOuterClass.DataProto.newBuilder()
            .setName("张三")
            .setAge(15)
            .addCourse("数学")
            .addCourse("英语")
            .build()

        Log.e("TAG--->","name::${data.name}")
        Log.e("TAG--->","age::${data.age}")
        data.courseList.forEachIndexed { index, value ->
            Log.e("TAG--->","course ::index${index} value::${value}")
        }
        //序列化并返回一个包含其原始字节的字节数组
        val byteArray: ByteArray = data.toByteArray()
        //反序列化从字节数组中解析消息
        val mData =DataProtoOuterClass.DataProto.parseFrom(byteArray)


        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent == null) {
            return
        }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val restartIntent = PendingIntent.getActivity(
            context,
            10010,
            launchIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 延迟执行（50~200ms）

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                restartIntent.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }

            finishAllActivities()
            // 杀死进程
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(0)
        }, 100)
    }


    /**
     * 销毁所有的 Activity
     *
     * @param classArray            白名单 Activity
     */
    @SafeVarargs
    fun finishAllActivities(vararg classArray: Class<out Activity>?) {
        val keys: Array<String?> = activitySet.keys.toTypedArray()
        for (key: String? in keys) {
            val activity: Activity? = activitySet[key]
            if (activity == null || activity.isFinishing) {
                continue
            }
            var whiteClazz = false
            for (clazz: Class<out Activity?>? in classArray) {
                if ((activity.javaClass == clazz)) {
                    whiteClazz = true
                }
            }
            if (whiteClazz) {
                continue
            }

            // 如果不是白名单上面的 Activity 就销毁掉
            activity.finish()
            activitySet.remove(key)
        }
    }

    val fragmentLifecycleCallback = FragmentLifecycleCallback()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity is FragmentActivity) {
            activity.supportFragmentManager
                .registerFragmentLifecycleCallbacks(fragmentLifecycleCallback, true)
        }
        LogUtil.i(String.format("%s - onCreate", activity.javaClass.simpleName))
        if (activitySet.size == 0) {
            for (callback: ApplicationLifecycleCallback? in lifecycleCallbacks) {
                callback?.onApplicationCreate(activity)
            }
            LogUtil.i(String.format("%s - onApplicationCreate", activity.javaClass.simpleName))
        }
        activitySet[getObjectTag(activity)] = activity
        topActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        LogUtil.i(String.format("%s - onStart", activity.javaClass.simpleName))
    }

    override fun onActivityResumed(activity: Activity) {
        LogUtil.i(String.format("%s - onResume", activity.javaClass.simpleName))
        if (topActivity === activity && resumedActivity == null) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationForeground(activity)
            }
            LogUtil.i(String.format("%s - onApplicationForeground", activity.javaClass.simpleName))
        }
        topActivity = activity
        resumedActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        LogUtil.i(String.format("%s - onPause", activity.javaClass.simpleName))
    }

    override fun onActivityStopped(activity: Activity) {
        LogUtil.i(String.format("%s - onStop", activity.javaClass.simpleName))
        if (resumedActivity === activity) {
            resumedActivity = null
        }
        if (resumedActivity == null) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationBackground(activity)
            }
            LogUtil.i(String.format("%s - onApplicationBackground", activity.javaClass.simpleName))
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        LogUtil.i(String.format("%s - onSaveInstanceState", activity.javaClass.simpleName))
    }

    override fun onActivityDestroyed(activity: Activity) {
        LogUtil.i(String.format("%s - onDestroy", activity.javaClass.simpleName))
        activitySet.remove(getObjectTag(activity))
        if (topActivity === activity) {
            topActivity = null
        }
        if (activitySet.isEmpty()) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationDestroy(activity)
            }
            LogUtil.i(String.format("%s - onApplicationDestroy", activity.javaClass.simpleName))
        }
    }

    inner class FragmentLifecycleCallback : FragmentManager.FragmentLifecycleCallbacks() {
        override fun onFragmentAttached(fm: FragmentManager, f: Fragment, context: Context) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentAttached"))
        }

        override fun onFragmentCreated(
            fm: FragmentManager,
            f: Fragment,
            savedInstanceState: Bundle?
        ) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentCreated"))
        }

        override fun onFragmentActivityCreated(
            fm: FragmentManager,
            f: Fragment,
            savedInstanceState: Bundle?
        ) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentActivityCreated"))
        }

        override fun onFragmentViewCreated(
            fm: FragmentManager,
            f: Fragment,
            v: View,
            savedInstanceState: Bundle?
        ) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentViewCreated"))
        }

        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentStarted"))
        }

        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentResumed"))
        }

        override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentPaused"))
        }

        override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentStopped"))
        }

        override fun onFragmentSaveInstanceState(
            fm: FragmentManager,
            f: Fragment,
            outState: Bundle
        ) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentSaveInstanceState"))
        }

        override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentViewDestroyed"))
        }

        override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentDestroyed"))
        }

        override fun onFragmentDetached(fm: FragmentManager, f: Fragment) {
            LogUtil.i(String.format("%s %s", f.javaClass.simpleName, "onFragmentDetached"))
        }
    }

    /**
     * 应用生命周期回调
     */
    interface ApplicationLifecycleCallback {

        /**
         * 第一个 Activity 创建了
         */
        fun onApplicationCreate(activity: Activity)

        /**
         * 最后一个 Activity 销毁了
         */
        fun onApplicationDestroy(activity: Activity)

        /**
         * 应用从前台进入到后台
         */
        fun onApplicationBackground(activity: Activity)

        /**
         * 应用从后台进入到前台
         */
        fun onApplicationForeground(activity: Activity)
    }
}