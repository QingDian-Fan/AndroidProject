package com.common.utils.keyboard

import android.app.Activity
import android.content.Context
import android.content.res.Configuration

class KeyboardCompat private constructor(activity: Activity) : OnKeyboardStateChangeListener {

    private val mProvider: KeyboardStateProvider = KeyboardStateProvider(activity)
    private val mPreferences: KeyboardHeightPreferences =
        KeyboardHeightPreferences.getInstance(activity)
    private var mOnStateChangeListener: OnStateChangeListener? = null

    init {
        mProvider.setKeyboardHeightObserver(this)
    }

    fun setOnStateChangeListener(onStateChangeListener: OnStateChangeListener?): KeyboardCompat {
        mOnStateChangeListener = onStateChangeListener
        return this
    }

    fun attach() {
        mProvider.start()
    }

    fun detach() {
        mProvider.close()
    }

    override fun onKeyboardStateChanged(isShown: Boolean, height: Int, orientation: Int) {
        mOnStateChangeListener?.onStateChanged(isShown, height, orientation)
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mPreferences.saveHeightPortrait(height)
        } else {
            mPreferences.saveHeightLandscape(height)
        }
    }

    interface OnStateChangeListener {
        fun onStateChanged(isShown: Boolean, height: Int, orientation: Int)
    }

    companion object {
        @JvmStatic
        fun getHeightPortrait(context: Context): Int {
            return KeyboardHeightPreferences.getInstance(context).getHeightPortrait()
        }

        @JvmStatic
        fun getHeightLandscape(context: Context): Int {
            return KeyboardHeightPreferences.getInstance(context).getHeightLandscape()
        }

        @JvmStatic
        fun with(activity: Activity): KeyboardCompat {
            return KeyboardCompat(activity)
        }
    }
}