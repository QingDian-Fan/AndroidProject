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

    enum class ButtonType {
        PRESS_HOLD,     // 长按录制（默认）
        TOGGLE_CLICK    // 点击开始/停止录制
    }

    private var type: ButtonType = ButtonType.PRESS_HOLD

    fun setButtonType(t: ButtonType) {
        type = t
    }

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
    private var isRecording = false
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
        val size = MeasureSpec.getSize(widthMeasureSpec)
            .coerceAtMost(MeasureSpec.getSize(heightMeasureSpec))
        setMeasuredDimension(size, size)
        outerRadius = size / 2f - outerPaint.strokeWidth / 2
        innerRadius = outerRadius - spacing - dpToPx(8f)
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f

        // 外圈
        canvas.drawCircle(cx, cy, outerRadius, outerPaint)

        if (!isRecording) {
            // 内圈圆
            canvas.drawCircle(cx, cy, innerRadius, innerPaint)
        } else {
            // 红色方块
            val side = outerRadius * 3 / 5f
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
        animator.interpolator = DecelerateInterpolator()
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
                when (type) {
                    ButtonType.PRESS_HOLD -> handlePressHoldDown()
                    ButtonType.TOGGLE_CLICK -> { /* do nothing on down */
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                when (type) {
                    ButtonType.PRESS_HOLD -> handlePressHoldUp()
                    ButtonType.TOGGLE_CLICK -> handleToggleClick()
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                if (type == ButtonType.PRESS_HOLD && isLongPressed) {
                    isLongPressed = false
                    isRecording = false
                    callback?.onEndRecord()
                    invalidate()
                }
            }
        }
        return true
    }

    // ------- PRESS_HOLD 模式逻辑 -------
    private fun handlePressHoldDown() {
        isTouch = true
        postDelayed({
            if (isTouch) {
                isLongPressed = true
                isRecording = true
                callback?.onStartRecord()
                invalidate()
            }
        }, 300)
    }

    private fun handlePressHoldUp() {
        isTouch = false
        removeCallbacks(null)
        if (isLongPressed) {
            isLongPressed = false
            isRecording = false
            callback?.onEndRecord()
            invalidate()
        } else {
            performClickAnimation()
            callback?.onTakePhoto()
        }
    }

    // ------- TOGGLE_CLICK 模式逻辑 -------
    private fun handleToggleClick() {
        if (!isRecording) {
            isRecording = true
            callback?.onStartRecord()
        } else {
            isRecording = false
            callback?.onEndRecord()
        }
        invalidate()
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
