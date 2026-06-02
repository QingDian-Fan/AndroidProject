package com.common.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceUtil {

    private const val CONFIG = "app_config"

    /**
     * 获取SharedPreferences实例对象
     */
    private fun getSharedPreference(fileName: String): SharedPreferences =
        Utils.getAppInstance().getSharedPreferences(fileName, Context.MODE_PRIVATE)

    /**
     * 保存一个String类型的值！
     */
    @JvmStatic
    fun putString(key: String, value: String) {
        getSharedPreference(CONFIG).edit().putString(key, value).apply()
    }

    /**
     * 获取String的value
     */
    @JvmStatic
    fun getString(key: String, defValue: String?): String? =
        getSharedPreference(CONFIG).getString(key, defValue)

    /**
     * 保存一个Boolean类型的值！
     */
    @JvmStatic
    fun putBoolean(key: String, value: Boolean) {
        getSharedPreference(CONFIG).edit().putBoolean(key, value).apply()
    }

    /**
     * 获取boolean的value
     */
    @JvmStatic
    fun getBoolean(key: String, defValue: Boolean): Boolean =
        getSharedPreference(CONFIG).getBoolean(key, defValue)

    /**
     * 保存一个int类型的值！
     */
    @JvmStatic
    fun putInt(key: String, value: Int) {
        getSharedPreference(CONFIG).edit().putInt(key, value).apply()
    }

    /**
     * 获取int的value
     */
    @JvmStatic
    fun getInt(key: String, defValue: Int): Int =
        getSharedPreference(CONFIG).getInt(key, defValue)

    /**
     * 保存一个float类型的值！
     */
    @JvmStatic
    fun putFloat(fileName: String, key: String, value: Float) {
        getSharedPreference(fileName).edit().putFloat(key, value).apply()
    }

    /**
     * 获取float的value
     */
    @JvmStatic
    fun getFloat(key: String, defValue: Float): Float =
        getSharedPreference(CONFIG).getFloat(key, defValue)

    /**
     * 保存一个long类型的值！
     */
    @JvmStatic
    fun putLong(key: String, value: Long) {
        getSharedPreference(CONFIG).edit().putLong(key, value).apply()
    }

    /**
     * 获取long的value
     */
    @JvmStatic
    fun getLong(key: String, defValue: Long): Long =
        getSharedPreference(CONFIG).getLong(key, defValue)

    /**
     * 取出List<String>
     *
     * @param key List<String> 对应的key
     * @return List<String>
     */
    @JvmStatic
    fun getStrListValue(key: String): List<String> {
        val strList = ArrayList<String>()
        val size = getInt(key + "size", 0)
        for (i in 0 until size) {
            getString(key + i, null)?.let { strList.add(it) }
        }
        return strList
    }

    /**
     * 存储List<String>
     *
     * @param key     List<String>对应的key
     * @param strList 对应需要存储的List<String>
     */
    @JvmStatic
    fun putStrListValue(key: String, strList: List<String>?) {
        if (strList == null) {
            return
        }
        // 保存之前先清理已经存在的数据，保证数据的唯一性
        removeStrList(key)
        val size = strList.size
        putInt(key + "size", size)
        for (i in 0 until size) {
            putString(key + i, strList[i])
        }
    }

    /**
     * 清空List<String>所有数据
     *
     * @param key List<String>对应的key
     */
    @JvmStatic
    fun removeStrList(key: String) {
        val size = getInt(key + "size", 0)
        if (size == 0) {
            return
        }
        remove(key + "size")
        for (i in 0 until size) {
            remove(key + i)
        }
    }

    /**
     * 清空对应key数据
     */
    @JvmStatic
    fun remove(key: String) {
        getSharedPreference(CONFIG).edit().remove(key).apply()
    }
}