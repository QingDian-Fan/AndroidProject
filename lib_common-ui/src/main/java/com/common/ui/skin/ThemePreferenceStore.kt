package com.common.ui.skin

import android.content.Context
import android.content.SharedPreferences
import com.common.utils.Utils
import androidx.core.content.edit

internal object ThemePreferenceStore {

    private val prefs: SharedPreferences by lazy {
        Utils.getAppInstance().getSharedPreferences(SkinConfig.SP_NAME, Context.MODE_PRIVATE)
    }

    fun getString(key: String, defValue: String): String =
        prefs.getString(key, defValue) ?: defValue

    fun putString(key: String, value: String) {
        prefs.edit { putString(key, value) }
    }
}