package com.dian.demo.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

class RectFrameLayout@JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null)  : FrameLayout(context,attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size,size)
    }

}