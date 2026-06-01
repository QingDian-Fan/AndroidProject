package com.common.weight

import android.R.attr.minHeight
import android.R.attr.minWidth
import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView

/**
 * Enhanced PercentImageView
 *
 * 功能：
 * 1. 基于宽 or 高 按百分比缩放
 * 2. 支持宽高比例 aspectRatio = width/height
 * 3. 支持 wrap_content 自动自适应
 * 4. 支持 maxWidth / maxHeight / minWidth / minHeight
 */
class PercentImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        const val BASICS_WIDTH = 0
        const val BASICS_HEIGHT = 1
    }

    private var basics: Int = BASICS_WIDTH
    private var percent: Float = 1f

    /** 新增：宽高比例（若设置，优先于 percent）*/
    private var aspectRatio: Float = 0f  // width/height

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.PercentImageView)
        basics = ta.getInt(R.styleable.PercentImageView_piv_basics, BASICS_WIDTH)
        percent = ta.getFloat(R.styleable.PercentImageView_piv_percent, 1f)
        aspectRatio = ta.getFloat(R.styleable.PercentImageView_piv_aspectRatio, 0f)
        ta.recycle()
    }

    fun setPercent(basics: Int, value: Float) {
        this.basics = basics
        this.percent = value
        aspectRatio = 0f // 避免冲突
        requestLayout()
    }

    fun setAspectRatio(ratio: Float) {
        aspectRatio = ratio
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)

        // 1. 若设置比例 aspectRatio，则优先使用
        if (aspectRatio > 0f) {
            if (width > 0 && height == 0) {
                height = (width / aspectRatio).toInt()
            } else if (height > 0 && width == 0) {
                width = (height * aspectRatio).toInt()
            } else {
                // 两者都有值，则以 basics 决定谁为基准
                when (basics) {
                    BASICS_WIDTH -> height = (width / aspectRatio).toInt()
                    BASICS_HEIGHT -> width = (height * aspectRatio).toInt()
                }
            }
            setMeasuredDimension(width, height)
            return
        }

        // 2. 使用 percent 模式
        when (basics) {
            BASICS_WIDTH -> height = (width * percent).toInt()
            BASICS_HEIGHT -> width = (height * percent).toInt()
        }

        // 3. 应用 min/max 限制（系统自带）
        width = width.coerceIn(minWidth, maxWidth)
        height = height.coerceIn(minHeight, maxHeight)

        setMeasuredDimension(width, height)
    }
}