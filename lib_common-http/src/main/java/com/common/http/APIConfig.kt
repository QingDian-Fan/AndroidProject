package com.common.http

import android.content.Context
import com.common.utils.ResourcesUtil
import com.common.utils.Utils
import com.common.utils.datastore.AppDataStore

object APIConfig {
    val DEBUG_URL_CONFIG = "common_debug_url"
    val BASE_URL: String = ResourcesUtil.getString(R.string.release_base_url)

    fun getBaseUrl(): String{
        return if (Utils.isDebug) {
            AppDataStore.getData(DEBUG_URL_CONFIG, BASE_URL)
        } else BASE_URL
    }

    fun toLoginActivity(){
        val companionClass = Class.forName("com.example.app.LoginActivity\$Companion")
        val companionInstance = companionClass.getDeclaredField("Companion").get(null)
        val startMethod = companionClass.getMethod("start", Context::class.java)
        startMethod.invoke(companionInstance, Utils.getAppContext())
    }
}