package com.common.weight

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class DrawableTextView: AppCompatTextView{
    private var drawableWidth:Int = 0
    private var drawableHeight:Int = 0

    constructor(context:Context):this(context,null)

    constructor(context:Context,attrs: AttributeSet?):this(context,attrs,0)

    constructor(context:Context,attrs: AttributeSet?,defStyleAttr:Int):super(context,attrs,defStyleAttr){
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DrawableTextView)
        drawableWidth = ta.getDimensionPixelSize(R.styleable.DrawableTextView_drawable_width,0)
        drawableHeight = ta.getDimensionPixelSize(R.styleable.DrawableTextView_drawable_height,0)
        ta.recycle()
    }

    fun setDrawableSize(width: Int, height: Int) {
        drawableWidth = width
        drawableHeight = height
        if (!isAttachedToWindow) {
            return
        }
        refreshDrawablesSize()
    }

    /**
     * 限定 Drawable 宽度
     */
    fun setDrawableWidth(width: Int) {
        drawableWidth = width
        if (!isAttachedToWindow) {
            return
        }
        refreshDrawablesSize()
    }

    /**
     * 限定 Drawable 高度
     */
    fun setDrawableHeight(height: Int) {
        drawableHeight = height
        if (!isAttachedToWindow) {
            return
        }
        refreshDrawablesSize()
    }

    override fun setCompoundDrawables(left: Drawable?, top: Drawable?, right: Drawable?, bottom: Drawable?) {
        super.setCompoundDrawables(left, top, right, bottom)
        if (!isAttachedToWindow) {
            return
        }
        refreshDrawablesSize()
    }


    override fun setCompoundDrawablesRelative(start: Drawable?, top: Drawable?, end: Drawable?, bottom: Drawable?) {
        super.setCompoundDrawablesRelative(start, top, end, bottom)
        if (!isAttachedToWindow) {
            return
        }
        refreshDrawablesSize()
    }

    /**
     * 刷新 Drawable 列表大小
     */
    private fun refreshDrawablesSize() {
        if (drawableWidth == 0 || drawableHeight == 0) {
            return
        }
        var compoundDrawables = compoundDrawables
        if (compoundDrawables[0] != null || compoundDrawables[1] != null) {
            super.setCompoundDrawables(
                limitDrawableSize(compoundDrawables[0]),
                limitDrawableSize(compoundDrawables[1]),
                limitDrawableSize(compoundDrawables[2]),
                limitDrawableSize(compoundDrawables[3])
            )
            return
        }
        compoundDrawables = compoundDrawablesRelative
        super.setCompoundDrawablesRelative(
            limitDrawableSize(compoundDrawables[0]),
            limitDrawableSize(compoundDrawables[1]),
            limitDrawableSize(compoundDrawables[2]),
            limitDrawableSize(compoundDrawables[3])
        )
    }

    /**
     * 重新限定 Drawable 宽高
     */
    private fun limitDrawableSize(drawable: Drawable?): Drawable? {
        if (drawable == null) {
            return null
        }
        if (drawableWidth == 0 || drawableWidth == 0) {
            return drawable
        }
        drawable.setBounds(0, 0, drawableWidth, drawableHeight)
        return drawable
    }


}