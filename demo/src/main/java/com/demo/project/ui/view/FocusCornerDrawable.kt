package com.demo.project.ui.view

import android.graphics.*
import android.graphics.drawable.Drawable


class FocusCornerDrawable(
    private val color: Int = Color.YELLOW,
    private val strokeWidth: Float = 4f,
    private val cornerLength: Float = 40f
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
        this.strokeWidth = this@FocusCornerDrawable.strokeWidth
        this.color = this@FocusCornerDrawable.color
    }

    override fun draw(canvas: Canvas) {
        val rect = RectF(bounds)
        val l = rect.left
        val t = rect.top
        val r = rect.right
        val b = rect.bottom
        val c = cornerLength

        // 左上角
        canvas.drawLine(l, t, l + c, t, paint)
        canvas.drawLine(l, t, l, t + c, paint)
        // 右上角
        canvas.drawLine(r, t, r - c, t, paint)
        canvas.drawLine(r, t, r, t + c, paint)
        // 左下角
        canvas.drawLine(l, b, l + c, b, paint)
        canvas.drawLine(l, b, l, b - c, paint)
        // 右下角
        canvas.drawLine(r, b, r - c, b, paint)
        canvas.drawLine(r, b, r, b - c, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
