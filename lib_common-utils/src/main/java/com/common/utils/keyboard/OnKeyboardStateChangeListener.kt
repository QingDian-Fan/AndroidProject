package com.common.utils.keyboard

interface OnKeyboardStateChangeListener {
    fun onKeyboardStateChanged(isShown: Boolean, height: Int, orientation: Int)
}