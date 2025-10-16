package com.dian.demo.utils

import android.Manifest
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
import android.os.Process
import androidx.annotation.RequiresPermission
import androidx.collection.ArrayMap
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

    @SuppressLint("ScheduleExactAlarm")
    fun restartAPP(context: Context) {

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (launchIntent == null) {
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val restartIntent = PendingIntent.getActivity(context, 10010, launchIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 延迟 800ms 启动，确保退出后系统有时间重新调度
        alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + 800, restartIntent)


        // 延迟一点点再退出，确保 Alarm 任务已注册
        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }, 200)
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

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        LogUtil.i("%s - onCreate", activity.javaClass.simpleName)
        if (activitySet.size == 0) {
            for (callback: ApplicationLifecycleCallback? in lifecycleCallbacks) {
                callback?.onApplicationCreate(activity)
            }
            LogUtil.i("%s - onApplicationCreate", activity.javaClass.simpleName)
        }
        activitySet[getObjectTag(activity)] = activity
        topActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        LogUtil.i("%s - onStart", activity.javaClass.simpleName)
    }

    override fun onActivityResumed(activity: Activity) {
        LogUtil.i("%s - onResume", activity.javaClass.simpleName)
        if (topActivity === activity && resumedActivity == null) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationForeground(activity)
            }
            LogUtil.i("%s - onApplicationForeground", activity.javaClass.simpleName)
        }
        topActivity = activity
        resumedActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {
        LogUtil.i("%s - onPause", activity.javaClass.simpleName)
    }

    override fun onActivityStopped(activity: Activity) {
        LogUtil.i("%s - onStop", activity.javaClass.simpleName)
        if (resumedActivity === activity) {
            resumedActivity = null
        }
        if (resumedActivity == null) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationBackground(activity)
            }
            LogUtil.i("%s - onApplicationBackground", activity.javaClass.simpleName)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        LogUtil.i("%s - onSaveInstanceState", activity.javaClass.simpleName)
    }

    override fun onActivityDestroyed(activity: Activity) {
        LogUtil.i("%s - onDestroy", activity.javaClass.simpleName)
        activitySet.remove(getObjectTag(activity))
        if (topActivity === activity) {
            topActivity = null
        }
        if (activitySet.size == 0) {
            for (callback: ApplicationLifecycleCallback in lifecycleCallbacks) {
                callback.onApplicationDestroy(activity)
            }
            LogUtil.i("%s - onApplicationDestroy", activity.javaClass.simpleName)
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