package com.common.ui.skin

enum class SkinNightMode(val key: String) {
    FOLLOW_SYSTEM("system"),
    DAY("day"),
    NIGHT("night");

    companion object {
        fun fromName(name: String?): SkinNightMode {
            return values().firstOrNull { it.key == name } ?: FOLLOW_SYSTEM
        }
    }
}
