package com.common.weight

import android.R
import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.LinearLayout

class CheckableItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs), Checkable {

    private var checked = false
    private var onChecked: ((view: CheckableItemView, isChecked: Boolean) -> Unit)? = null

    fun setListener(onChecked:(view: CheckableItemView, isChecked: Boolean) -> Unit){
        this.onChecked = onChecked
    }

    init {
        isClickable = true
        isFocusable = true
    }

    override fun isChecked(): Boolean = checked

    override fun setChecked(checked: Boolean) {
        if (this.checked == checked) return
        this.checked = checked
        onChecked?.invoke(this,checked)
        refreshDrawableState()
        for (i in 0..childCount){
            getChildAt(i)?.isSelected = checked
        }
    }

    override fun toggle() {
        setChecked(!checked)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (checked) {
            mergeDrawableStates(drawableState, CHECKED_STATE)
        }
        return drawableState
    }

    companion object {
        private val CHECKED_STATE = intArrayOf(R.attr.state_checked)
    }
}


