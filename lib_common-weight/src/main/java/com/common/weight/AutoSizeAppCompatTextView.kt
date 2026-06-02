package com.common.weight

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import android.util.TypedValue
import android.text.TextPaint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextUtils
import com.common.weight.R
import kotlin.math.max
import kotlin.math.min

class AutoSizeAppCompatTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var minTextSizePx = 12f      // 最小字体
    private var maxTextSizePx = textSize // 最大字体
    private var stepGranularity = 1f     // 每次缩放的步长

    private var needsAutoSize = true

    init {
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.AutoSizeAppCompatTextView)
            minTextSizePx = a.getDimension(R.styleable.AutoSizeAppCompatTextView_minTextSize, minTextSizePx)
            maxTextSizePx = a.getDimension(R.styleable.AutoSizeAppCompatTextView_maxTextSize, maxTextSizePx)
            stepGranularity = a.getDimension(R.styleable.AutoSizeAppCompatTextView_stepGranularity, stepGranularity)
            a.recycle()
        }

        setSingleLine(false)
        ellipsize = TextUtils.TruncateAt.END
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        needsAutoSize = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        needsAutoSize = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (needsAutoSize) {
            autoSizeText(right - left, bottom - top)
            needsAutoSize = false
        }
    }

    private fun autoSizeText(width: Int, height: Int) {
        if (width <= 0 || height <= 0 || text.isNullOrEmpty()) return

        var targetSize = maxTextSizePx
        val paint = TextPaint(paint)
        val availableWidth = width - paddingLeft - paddingRight
        val availableHeight = height - paddingTop - paddingBottom

        while (targetSize >= minTextSizePx) {
            paint.textSize = targetSize
            val layout = createStaticLayout(text.toString(), paint, availableWidth)
            if (layout.height <= availableHeight) {
                break
            }
            targetSize -= stepGranularity
        }

        setTextSize(TypedValue.COMPLEX_UNIT_PX, max(targetSize, minTextSizePx))
    }

    private fun createStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                .setIncludePad(includeFontPadding)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(text, paint, width, Layout.Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineSpacingExtra, includeFontPadding)
        }
    }
}