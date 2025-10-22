package com.dian.demo.ui.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

class CameraButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = dpToPx(4f)
    }

    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

    private val spacing = dpToPx(2f)
    private var outerRadius = 0f
    private var innerRadius = 0f

    private var isLongPressed = false
    private var callback: CameraButtonCallback? = null

    fun setCameraButtonCallback(cb: CameraButtonCallback) {
        callback = cb
    }

    interface CameraButtonCallback {
        fun onTakePhoto()
        fun onStartRecord()
        fun onEndRecord()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec).coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
        outerRadius = size / 2f - outerPaint.strokeWidth / 2
        innerRadius = outerRadius - spacing - dpToPx(8f)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        // 绘制外圈
        canvas.drawCircle(cx, cy, outerRadius, outerPaint)

        if (!isLongPressed) {
            // 绘制内圈圆
            canvas.drawCircle(cx, cy, innerRadius, innerPaint)
        } else {
            // 绘制红色正方形
            val side = outerRadius * 3 / 5f // 外圈直径的 1/5
            val rect = RectF(
                cx - side / 2,
                cy - side / 2,
                cx + side / 2,
                cy + side / 2
            )
            canvas.drawRoundRect(rect, dpToPx(1f), dpToPx(1f), squarePaint)

        }
    }

    private fun performClickAnimation() {
        val animator = ValueAnimator.ofFloat(1f, 0.8f, 1f)
        animator.duration = 150
        animator.addUpdateListener {
            val scale = it.animatedValue as Float
            innerRadius = (outerRadius - spacing - dpToPx(8f)) * scale
            invalidate()
        }
        animator.start()
    }
    private var isTouch = false
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 延迟 300ms 检测长按
                isTouch = true
                postDelayed({
                    if (isTouch) {
                        isLongPressed = true
                        callback?.onStartRecord()
                        invalidate()
                    }
                }, 300)
            }
            MotionEvent.ACTION_UP -> {
                isTouch = false
                removeCallbacks(null)
                if (isLongPressed) {
                    // 长按结束
                    isLongPressed = false
                    callback?.onEndRecord()
                    invalidate()
                } else {
                    // 普通点击
                    performClickAnimation()
                    callback?.onTakePhoto()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                isTouch = false
                removeCallbacks(null)
                if (isLongPressed) {
                    isLongPressed = false
                    callback?.onEndRecord()
                    invalidate()
                }
            }
        }
        return true
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
