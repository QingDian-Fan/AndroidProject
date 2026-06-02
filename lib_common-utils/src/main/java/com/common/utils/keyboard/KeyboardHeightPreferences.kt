package com.common.utils.keyboard

import android.content.Context
import android.content.SharedPreferences

internal class KeyboardHeightPreferences private constructor(context: Context) {

    private val mPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    fun getHeightPortrait(): Int {
        return mPreferences.getInt(KEY_HEIGHT_PORTRAIT, 0)
    }

    fun getHeightLandscape(): Int {
        return mPreferences.getInt(KEY_HEIGHT_LANDSCAPE, 0)
    }

    fun saveHeightPortrait(height: Int) {
        mPreferences.edit().putInt(KEY_HEIGHT_PORTRAIT, height).apply()
    }

    fun saveHeightLandscape(height: Int) {
        mPreferences.edit().putInt(KEY_HEIGHT_LANDSCAPE, height).apply()
    }

    companion object {
        private const val SP_NAME = "keyboard_height"
        private const val KEY_HEIGHT_PORTRAIT = "height_portrait"
        private const val KEY_HEIGHT_LANDSCAPE = "height_landscape"

        @Volatile
        private var INSTANCE: KeyboardHeightPreferences? = null

        fun getInstance(context: Context): KeyboardHeightPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeyboardHeightPreferences(context).also { INSTANCE = it }
            }
        }
    }
}