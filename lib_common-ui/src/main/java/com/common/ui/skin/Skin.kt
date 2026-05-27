package com.common.ui.skin

import com.common.ui.R

enum class Skin(val key: String, val label: String, val themeRes: Int) {
    BLUE("blue", "Blue", R.style.Theme_SkinDemo_Blue),
    RED("red", "Red", R.style.Theme_SkinDemo_Red),
    GREEN("green", "Green", R.style.Theme_SkinDemo_Green),
    GOLD("gold", "Gold", R.style.Theme_SkinDemo_Gold);

    companion object {
        fun fromName(name: String?): Skin {
            return values().firstOrNull { it.key == name } ?: BLUE
        }
    }
}
