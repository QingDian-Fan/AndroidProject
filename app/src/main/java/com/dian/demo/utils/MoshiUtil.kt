package com.dian.demo.utils

import android.util.Log
import com.dian.demo.http.moshi.NullSafeKotlinJsonAdapterFactory
import com.dian.demo.http.moshi.NullSafeStandardJsonAdapters
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType



object MoshiUtil {

    val moshi: Moshi = Moshi.Builder()
        .add(NullSafeStandardJsonAdapters.FACTORY)
        .add(NullSafeKotlinJsonAdapterFactory()).build()

     fun <K, V> objectsMapToJson(key: Class<K>, value: Class<V>, map: Map<K,V>): String {
        return moshi
            .adapter<Map<K,V>>(Types.newParameterizedType(Map::class.java, key, value))
            .toJson(map)
    }


    fun <T> toJson(adapter: JsonAdapter<T>, src: T, indent: String = ""): String {
        try {
            return adapter.indent(indent).toJson(src)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""

    }

    /**
     * T 类型对象序列化为 json
     * @param src T
     * @param indent String
     * @return String
     */
    inline fun <reified T> toJson(src: T, indent: String = ""): String {
        val adapter = moshi.adapter(T::class.java)
        return this.toJson(adapter = adapter, src = src, indent = indent)
    }


    /**
     * 将 T 序列化为 json，指定 parameterizedType，适合复杂类型
     * @param src T
     * @param parameterizedType ParameterizedType
     * @param indent String
     * @return String
     */
    inline fun <reified T> toJson(src: T, parameterizedType: ParameterizedType, indent: String = ""): String {
        val adapter = moshi.adapter<T>(parameterizedType)
        return this.toJson(adapter = adapter, src = src, indent = indent)
    }

    inline fun <reified T> fromJson(adapter: JsonAdapter<T>, jsonStr: String): T? {
        try {
            return adapter.fromJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * json 反序列化为 T
     * @param jsonStr String
     * @return T?
     */
    inline fun <reified T> fromJson(jsonStr: String): T? {
        Log.e("TAGTAG","TEST1")
        val adapter = moshi.adapter(T::class.java)
        Log.e("TAGTAG","TEST"+adapter.toString())
        return this.fromJson(adapter, jsonStr)
    }

    /**
     * json 反序列化为 MutableList<T>
     * @param jsonStr String
     * @return MutableList<T>?
     */
    inline fun <reified T> fromJsonToList(jsonStr: String): MutableList<T>? {
        val parameterizedType = Types.newParameterizedType(MutableList::class.java, T::class.java)
        return fromJson<MutableList<T>>(jsonStr, parameterizedType)
    }

    /**
     * json 反序列化为 T, 指定 parameterizedType，复杂数据用
     * @param jsonStr String
     * @param parameterizedType ParameterizedType
     * @return T?
     */
    inline fun <reified T> fromJson(jsonStr: String, parameterizedType: Type): T? {
        val adapter = moshi.adapter<T>(parameterizedType)
        return this.fromJson(adapter = adapter, jsonStr = jsonStr)
    }

}

