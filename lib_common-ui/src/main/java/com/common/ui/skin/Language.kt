package com.common.ui.skin

import java.util.Locale

enum class Language(val key: String, val locale: Locale?) {
    SYSTEM("system", null),
    CHINESE("zh", Locale.SIMPLIFIED_CHINESE),
    ENGLISH("en", Locale.ENGLISH);

    companion object {
        fun fromName(name: String?): Language {
            return values().firstOrNull { it.key == name } ?: SYSTEM
        }
    }
}