package com.common.theme

import androidx.appcompat.app.AppCompatDelegate

enum class NightMode(val storageKey: String, val delegateMode: Int) {
    FOLLOW_SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    NIGHT("night", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        fun fromStorageKey(storageKey: String?): NightMode {
            return entries.firstOrNull { it.storageKey == storageKey } ?: FOLLOW_SYSTEM
        }
    }
}
