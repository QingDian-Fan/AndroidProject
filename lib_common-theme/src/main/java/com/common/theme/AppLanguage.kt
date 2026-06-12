package com.common.theme

enum class AppLanguage(val storageKey: String, val languageTags: String) {
    FOLLOW_SYSTEM("system", ""),
    CHINESE("zh", "zh-CN"),
    ENGLISH("en", "en");

    companion object {
        fun fromStorageKey(storageKey: String?): AppLanguage {
            return values().firstOrNull { it.storageKey == storageKey } ?: FOLLOW_SYSTEM
        }
    }
}
