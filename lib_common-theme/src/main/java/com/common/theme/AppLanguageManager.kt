package com.common.theme

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppLanguageManager {
    private const val PREFS_NAME = "common_theme_language"
    private const val KEY_LANGUAGE = "language"

    private var initialized = false
    private var language: AppLanguage = AppLanguage.FOLLOW_SYSTEM

    @JvmStatic
    val currentLanguage: AppLanguage
        get() = language

    @JvmStatic
    fun init(context: Context) {
        val appContext = context.applicationContext
        language = readLanguage(appContext)
        AppCompatDelegate.setApplicationLocales(language.toLocaleList())
        initialized = true
    }

    @JvmStatic
    fun setLanguage(context: Context, appLanguage: AppLanguage) {
        val appContext = context.applicationContext
        if (!initialized) {
            init(appContext)
        }
        if (language == appLanguage) {
            return
        }
        language = appLanguage
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, appLanguage.storageKey)
            .apply()
        AppCompatDelegate.setApplicationLocales(appLanguage.toLocaleList())
    }

    private fun readLanguage(context: Context): AppLanguage {
        val storageKey = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.FOLLOW_SYSTEM.storageKey)
        return AppLanguage.fromStorageKey(storageKey)
    }

    private fun AppLanguage.toLocaleList(): LocaleListCompat {
        return if (languageTags.isEmpty()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTags)
        }
    }
}
