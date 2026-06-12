package com.common.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object NightModeManager {
    private const val PREFS_NAME = "common_theme_night_mode"
    private const val KEY_NIGHT_MODE = "night_mode"

    private var initialized = false
    private var mode: NightMode = NightMode.FOLLOW_SYSTEM

    @JvmStatic
    val currentMode: NightMode
        get() = mode

    @JvmStatic
    fun init(context: Context) {
        val appContext = context.applicationContext
        mode = readMode(appContext)
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
        initialized = true
    }

    @JvmStatic
    fun setMode(context: Context, nightMode: NightMode) {
        val appContext = context.applicationContext
        if (!initialized) {
            init(appContext)
        }
        if (mode == nightMode) {
            return
        }
        mode = nightMode
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NIGHT_MODE, nightMode.storageKey)
            .apply()
        AppCompatDelegate.setDefaultNightMode(nightMode.delegateMode)
    }

    private fun readMode(context: Context): NightMode {
        val storageKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_NIGHT_MODE, NightMode.FOLLOW_SYSTEM.storageKey)
        return NightMode.fromStorageKey(storageKey)
    }
}
