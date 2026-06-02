@file:Suppress("DEPRECATION")

package com.common.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.Layout
import android.text.Layout.Alignment
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.MaskFilterSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ReplacementSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

/**
 * 主要功能:用于设置文字的前景色、背景色、Typeface、粗体、斜体、字号、超链接、删除线、下划线、上下标等
 */
object SpannableStringUtil {

    const val ALIGN_BOTTOM = 0
    const val ALIGN_BASELINE = 1
    const val ALIGN_CENTER = 2
    const val ALIGN_TOP = 3

    @IntDef(ALIGN_BOTTOM, ALIGN_BASELINE, ALIGN_CENTER, ALIGN_TOP)
    @Retention(AnnotationRetention.SOURCE)
    annotation class Align

    private val LINE_SEPARATOR: String? = System.getProperty("line.separator")

    class Builder {

        private val defaultValue = 0x12000000

        private var text: CharSequence? = null
        private var flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE

        @ColorInt
        private var foregroundColor = defaultValue

        @ColorInt
        private var backgroundColor = defaultValue

        @ColorInt
        private var quoteColor = defaultValue
        private var stripeWidth = 0
        private var quoteGapWidth = 0
        private var isLeadingMargin = false
        private var first = 0
        private var rest = 0
        private var margin = -1
        private var isBullet = false
        private var bulletColor = 0
        private var bulletRadius = 0
        private var bulletGapWidth = 0
        private var fontSize = -1
        private var fontSizeIsDp = false
        private var proportion = -1f
        private var xProportion = -1f
        private var isStrikethrough = false
        private var isUnderline = false
        private var isSuperscript = false
        private var isSubscript = false
        private var isBold = false
        private var isItalic = false
        private var isBoldItalic = false
        private var fontFamily: String? = null
        private var typeface: Typeface? = null
        private var alignment: Alignment? = null
        private var imageIsBitmap = false
        private var bitmap: Bitmap? = null
        private var imageIsDrawable = false
        private var drawable: Drawable? = null
        private var imageIsUri = false
        private var uri: Uri? = null
        private var imageIsResourceId = false

        @DrawableRes
        private var resourceId = 0

        @Align
        private var align = ALIGN_BOTTOM

        private var clickSpan: ClickableSpan? = null
        private var url: String? = null

        private var isBlur = false
        private var blurRadius = 0f
        private var style: BlurMaskFilter.Blur? = null

        private val mBuilder = SpannableStringBuilder()

        /**
         * 设置标识
         */
        fun setFlag(flag: Int): Builder {
            this.flag = flag
            return this
        }

        /**
         * 设置前景色
         */
        fun setForegroundColor(@ColorInt color: Int): Builder {
            this.foregroundColor = color
            return this
        }

        /**
         * 设置背景色
         */
        fun setBackgroundColor(@ColorInt color: Int): Builder {
            this.backgroundColor = color
            return this
        }

        /**
         * 设置引用线的颜色
         */
        fun setQuoteColor(@ColorInt color: Int): Builder {
            this.quoteColor = color
            this.stripeWidth = 2
            this.quoteGapWidth = 2
            return this
        }

        /**
         * 设置引用线的颜色
         */
        fun setQuoteColor(@ColorInt color: Int, stripeWidth: Int, quoteGapWidth: Int): Builder {
            this.quoteColor = color
            this.stripeWidth = stripeWidth
            this.quoteGapWidth = quoteGapWidth
            return this
        }

        /**
         * 设置缩进
         */
        fun setLeadingMargin(first: Int, rest: Int): Builder {
            this.first = first
            this.rest = rest
            isLeadingMargin = true
            return this
        }

        /**
         * 设置间距
         */
        fun setMargin(margin: Int): Builder {
            this.margin = margin
            this.text = " " + this.text
            return this
        }

        /**
         * 设置列表标记
         */
        fun setBullet(@ColorInt gapWidth: Int): Builder {
            this.bulletColor = 0
            this.bulletRadius = 3
            this.bulletGapWidth = gapWidth
            isBullet = true
            return this
        }

        /**
         * 设置列表标记
         */
        fun setBullet(@ColorInt color: Int, radius: Int, gapWidth: Int): Builder {
            this.bulletColor = color
            this.bulletRadius = radius
            this.bulletGapWidth = gapWidth
            isBullet = true
            return this
        }

        /**
         * 设置字体尺寸
         */
        fun setFontSize(size: Int): Builder {
            this.fontSize = size
            this.fontSizeIsDp = false
            return this
        }

        /**
         * 设置字体尺寸
         */
        fun setFontSize(size: Int, isDp: Boolean): Builder {
            this.fontSize = size
            this.fontSizeIsDp = isDp
            return this
        }

        /**
         * 设置字体比例
         */
        fun setFontProportion(proportion: Float): Builder {
            this.proportion = proportion
            return this
        }

        /**
         * 设置字体横向比例
         */
        fun setFontXProportion(proportion: Float): Builder {
            this.xProportion = proportion
            return this
        }

        /**
         * 设置删除线
         */
        fun setStrikethrough(): Builder {
            this.isStrikethrough = true
            return this
        }

        /**
         * 设置下划线
         */
        fun setUnderline(): Builder {
            this.isUnderline = true
            return this
        }

        /**
         * 设置上标
         */
        fun setSuperscript(): Builder {
            this.isSuperscript = true
            return this
        }

        /**
         * 设置下标
         */
        fun setSubscript(): Builder {
            this.isSubscript = true
            return this
        }

        /**
         * 设置粗体
         */
        fun setBold(): Builder {
            isBold = true
            return this
        }

        /**
         * 设置斜体
         */
        fun setItalic(): Builder {
            isItalic = true
            return this
        }

        /**
         * 设置粗斜体
         */
        fun setBoldItalic(): Builder {
            isBoldItalic = true
            return this
        }

        /**
         * 设置字体系列
         */
        fun setFontFamily(fontFamily: String): Builder {
            this.fontFamily = fontFamily
            return this
        }

        /**
         * 设置字体
         */
        fun setTypeface(typeface: Typeface): Builder {
            this.typeface = typeface
            return this
        }

        /**
         * 设置对齐
         */
        fun setAlign(alignment: Alignment): Builder {
            this.alignment = alignment
            return this
        }

        /**
         * 设置图片
         */
        fun setBitmap(bitmap: Bitmap): Builder {
            return setBitmap(bitmap, align)
        }

        /**
         * 设置图片
         */
        fun setBitmap(bitmap: Bitmap, @Align align: Int): Builder {
            this.bitmap = bitmap
            this.align = align
            this.text = " " + this.text
            imageIsBitmap = true
            return this
        }

        /**
         * 设置图片
         */
        fun setDrawable(drawable: Drawable): Builder {
            return setDrawable(drawable, align)
        }

        /**
         * 设置图片
         */
        fun setDrawable(drawable: Drawable, @Align align: Int): Builder {
            this.drawable = drawable
            this.align = align
            this.text = " " + this.text
            imageIsDrawable = true
            return this
        }

        /**
         * 设置图片
         */
        fun setUri(uri: Uri): Builder {
            setUri(uri, ALIGN_BOTTOM)
            return this
        }

        /**
         * 设置图片
         */
        fun setUri(uri: Uri, @Align align: Int): Builder {
            this.uri = uri
            this.align = align
            this.text = " " + this.text
            imageIsUri = true
            return this
        }

        /**
         * 设置图片
         */
        fun setResourceId(@DrawableRes resourceId: Int): Builder {
            return setResourceId(resourceId, align)
        }

        /**
         * 设置图片
         */
        fun setResourceId(@DrawableRes resourceId: Int, @Align align: Int): Builder {
            this.resourceId = resourceId
            this.align = align
            this.text = " " + this.text
            imageIsResourceId = true
            return this
        }

        /**
         * 设置点击事件
         * 需添加view.setMovementMethod(LinkMovementMethod.getInstance())
         */
        fun setClickSpan(clickSpan: ClickableSpan): Builder {
            this.clickSpan = clickSpan
            return this
        }

        /**
         * 设置超链接
         * 需添加view.setMovementMethod(LinkMovementMethod.getInstance())
         */
        fun setUrl(url: String): Builder {
            this.url = url
            return this
        }

        /**
         * 设置模糊
         */
        fun setBlur(radius: Float, style: BlurMaskFilter.Blur): Builder {
            this.blurRadius = radius
            this.style = style
            this.isBlur = true
            return this
        }

        /**
         * 追加样式一行字符串
         */
        fun appendLine(text: CharSequence): Builder {
            return append(text.toString() + LINE_SEPARATOR)
        }

        /**
         * 追加样式字符串
         */
        fun append(text: CharSequence): Builder {
            setSpan()
            this.text = text
            return this
        }

        /**
         * 创建样式字符串
         */
        fun create(): SpannableStringBuilder {
            setSpan()
            return mBuilder
        }

        /**
         * 设置样式
         */
        private fun setSpan() {
            val text = this.text
            if (text.isNullOrEmpty()) return
            val start = mBuilder.length
            mBuilder.append(text)
            val end = mBuilder.length
            if (clickSpan != null) {
                mBuilder.setSpan(clickSpan, start, end, flag)
                mBuilder.setSpan(NoUnderlineSpan(), start, end, flag)
                clickSpan = null
            }
            if (backgroundColor != defaultValue) {
                mBuilder.setSpan(BackgroundColorSpan(backgroundColor), start, end, flag)
                backgroundColor = defaultValue
            }
            if (foregroundColor != defaultValue) {
                mBuilder.setSpan(ForegroundColorSpan(foregroundColor), start, end, flag)
                foregroundColor = defaultValue
            }
            if (isLeadingMargin) {
                mBuilder.setSpan(LeadingMarginSpan.Standard(first, rest), start, end, flag)
                isLeadingMargin = false
            }
            if (margin != -1) {
                mBuilder.setSpan(MarginSpan(margin), start, end, flag)
                margin = -1
            }
            if (quoteColor != defaultValue) {
                mBuilder.setSpan(CustomQuoteSpan(quoteColor, stripeWidth, quoteGapWidth), start, end, flag)
                quoteColor = defaultValue
            }
            if (isBullet) {
                mBuilder.setSpan(CustomBulletSpan(bulletColor, bulletRadius, bulletGapWidth), start, end, flag)
                isBullet = false
            }
            if (fontSize != -1) {
                mBuilder.setSpan(AbsoluteSizeSpan(fontSize, fontSizeIsDp), start, end, flag)
                fontSize = -1
                fontSizeIsDp = false
            }
            if (proportion != -1f) {
                mBuilder.setSpan(RelativeSizeSpan(proportion), start, end, flag)
                proportion = -1f
            }
            if (xProportion != -1f) {
                mBuilder.setSpan(ScaleXSpan(xProportion), start, end, flag)
                xProportion = -1f
            }
            if (isStrikethrough) {
                mBuilder.setSpan(StrikethroughSpan(), start, end, flag)
                isStrikethrough = false
            }
            if (isUnderline) {
                mBuilder.setSpan(UnderlineSpan(), start, end, flag)
                isUnderline = false
            }
            if (isSuperscript) {
                mBuilder.setSpan(SuperscriptSpan(), start, end, flag)
                isSuperscript = false
            }
            if (isSubscript) {
                mBuilder.setSpan(SubscriptSpan(), start, end, flag)
                isSubscript = false
            }
            if (isBold) {
                mBuilder.setSpan(StyleSpan(Typeface.BOLD), start, end, flag)
                isBold = false
            }
            if (isItalic) {
                mBuilder.setSpan(StyleSpan(Typeface.ITALIC), start, end, flag)
                isItalic = false
            }
            if (isBoldItalic) {
                mBuilder.setSpan(StyleSpan(Typeface.BOLD_ITALIC), start, end, flag)
                isBoldItalic = false
            }
            fontFamily?.let {
                mBuilder.setSpan(TypefaceSpan(it), start, end, flag)
                fontFamily = null
            }
            typeface?.let {
                mBuilder.setSpan(CustomTypefaceSpan(it), start, end, flag)
                typeface = null
            }
            alignment?.let {
                mBuilder.setSpan(AlignmentSpan.Standard(it), start, end, flag)
                alignment = null
            }
            if (imageIsBitmap || imageIsDrawable || imageIsUri || imageIsResourceId) {
                if (imageIsBitmap) {
                    mBuilder.setSpan(CustomImageSpan(Utils.getAppContext(), bitmap, align), start, end, flag)
                    bitmap = null
                    imageIsBitmap = false
                } else if (imageIsDrawable) {
                    drawable?.let { mBuilder.setSpan(CustomImageSpan(it, align), start, end, flag) }
                    drawable = null
                    imageIsDrawable = false
                } else if (imageIsUri) {
                    uri?.let { mBuilder.setSpan(CustomImageSpan(Utils.getAppContext(), it, align), start, end, flag) }
                    uri = null
                    imageIsUri = false
                } else {
                    mBuilder.setSpan(CustomImageSpan(Utils.getAppContext(), resourceId, align), start, end, flag)
                    resourceId = 0
                    imageIsResourceId = false
                }
            }
            url?.let {
                mBuilder.setSpan(URLSpan(it), start, end, flag)
                url = null
            }
            if (isBlur) {
                mBuilder.setSpan(MaskFilterSpan(BlurMaskFilter(blurRadius, style)), start, end, flag)
                isBlur = false
            }
            flag = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        }
    }

    internal class NoUnderlineSpan : UnderlineSpan() {
        override fun updateDrawState(ds: TextPaint) {
            ds.color = ds.linkColor
            ds.isUnderlineText = false
        }
    }

    internal class MarginSpan(private val margin: Int) : ReplacementSpan() {

        override fun getSize(
            paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?
        ): Int {
            return margin
        }

        override fun draw(
            canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float,
            top: Int, y: Int, bottom: Int, paint: Paint
        ) {
        }
    }

    internal class CustomQuoteSpan(
        @ColorInt private val color: Int,
        private val stripeWidth: Int,
        private val gapWidth: Int
    ) : LeadingMarginSpan {

        override fun getLeadingMargin(first: Boolean): Int {
            return stripeWidth + gapWidth
        }

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int,
            first: Boolean, layout: Layout
        ) {
            val style = p.style
            val color = p.color

            p.style = Paint.Style.FILL
            p.color = this.color

            c.drawRect(x.toFloat(), top.toFloat(), (x + dir * stripeWidth).toFloat(), bottom.toFloat(), p)

            p.style = style
            p.color = color
        }
    }

    internal class CustomBulletSpan(
        private val color: Int,
        private val radius: Int,
        private val gapWidth: Int
    ) : LeadingMarginSpan {

        override fun getLeadingMargin(first: Boolean): Int {
            return 2 * radius + gapWidth
        }

        override fun drawLeadingMargin(
            c: Canvas, p: Paint, x: Int, dir: Int,
            top: Int, baseline: Int, bottom: Int,
            text: CharSequence, start: Int, end: Int,
            first: Boolean, l: Layout
        ) {
            if ((text as Spanned).getSpanStart(this) == start) {
                val style = p.style
                val oldColor = p.color
                p.color = color
                p.style = Paint.Style.FILL
                if (c.isHardwareAccelerated) {
                    val path = sBulletPath ?: Path().also {
                        // Bullet is slightly better to avoid aliasing artifacts on mdpi devices.
                        it.addCircle(0.0f, 0.0f, radius.toFloat(), Path.Direction.CW)
                        sBulletPath = it
                    }
                    c.save()
                    c.translate(x + dir * radius.toFloat(), (top + bottom) / 2.0f)
                    c.drawPath(path, p)
                    c.restore()
                } else {
                    c.drawCircle(x + dir * radius.toFloat(), (top + bottom) / 2.0f, radius.toFloat(), p)
                }
                p.color = oldColor
                p.style = style
            }
        }

        companion object {
            private var sBulletPath: Path? = null
        }
    }

    @SuppressLint("ParcelCreator")
    internal class CustomTypefaceSpan(private val newType: Typeface) : TypefaceSpan("") {

        override fun updateDrawState(textPaint: TextPaint) {
            apply(textPaint, newType)
        }

        override fun updateMeasureState(paint: TextPaint) {
            apply(paint, newType)
        }

        companion object {
            private fun apply(paint: Paint, tf: Typeface) {
                val old = paint.typeface
                val oldStyle = old?.style ?: 0

                val fake = oldStyle and tf.style.inv()
                if (fake and Typeface.BOLD != 0) {
                    paint.isFakeBoldText = true
                }
                if (fake and Typeface.ITALIC != 0) {
                    paint.textSkewX = -0.25f
                }
                paint.typeface = tf
            }
        }
    }

    internal class CustomImageSpan : CustomDynamicDrawableSpan {

        private var mDrawable: Drawable? = null
        private var mContentUri: Uri? = null
        private var mResourceId = 0
        private var mContext: Context? = null

        constructor(context: Context?, b: Bitmap?, verticalAlignment: Int) : super(verticalAlignment) {
            mContext = context
            val d = if (context != null) {
                BitmapDrawable(context.resources, b)
            } else {
                BitmapDrawable(b)
            }
            mDrawable = d
            val width = d.intrinsicWidth
            val height = d.intrinsicHeight
            d.setBounds(0, 0, if (width > 0) width else 0, if (height > 0) height else 0)
        }

        constructor(d: Drawable, verticalAlignment: Int) : super(verticalAlignment) {
            mDrawable = d
            d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
        }

        constructor(context: Context, uri: Uri, verticalAlignment: Int) : super(verticalAlignment) {
            mContext = context
            mContentUri = uri
        }

        constructor(context: Context, @DrawableRes resourceId: Int, verticalAlignment: Int) : super(verticalAlignment) {
            mContext = context
            mResourceId = resourceId
        }

        override fun getDrawable(): Drawable? {
            mDrawable?.let { return it }
            val context = mContext ?: return null
            val contentUri = mContentUri
            var drawable: Drawable? = null
            if (contentUri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(contentUri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    val d = BitmapDrawable(context.resources, bitmap)
                    d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
                    drawable = d
                    inputStream?.close()
                } catch (e: Exception) {
                    Log.e("sms", "Failed to loaded content $contentUri", e)
                }
            } else {
                try {
                    val d = ContextCompat.getDrawable(context, mResourceId)
                    drawable = d
                    d?.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
                } catch (e: Exception) {
                    Log.e("sms", "Unable to find resource: $mResourceId")
                }
            }
            return drawable
        }
    }

    internal abstract class CustomDynamicDrawableSpan : ReplacementSpan {

        private val mVerticalAlignment: Int

        constructor() {
            mVerticalAlignment = ALIGN_BOTTOM
        }

        constructor(verticalAlignment: Int) {
            mVerticalAlignment = verticalAlignment
        }

        abstract fun getDrawable(): Drawable?

        override fun getSize(
            paint: Paint, text: CharSequence?,
            start: Int, end: Int,
            fm: Paint.FontMetricsInt?
        ): Int {
            val d = getCachedDrawable() ?: return 0
            val rect = d.bounds
            val fontHeight = (paint.fontMetrics.descent - paint.fontMetrics.ascent).toInt()
            if (fm != null) { // this is the fucking code which I waste 3 days
                if (rect.height() > fontHeight) {
                    when (mVerticalAlignment) {
                        ALIGN_TOP -> fm.descent += rect.height() - fontHeight
                        ALIGN_CENTER -> {
                            fm.ascent -= (rect.height() - fontHeight) / 2
                            fm.descent += (rect.height() - fontHeight) / 2
                        }
                        ALIGN_BASELINE -> fm.ascent -= rect.height() - fontHeight + fm.descent
                        else -> fm.ascent -= rect.height() - fontHeight
                    }
                }
            }
            return rect.right
        }

        override fun draw(
            canvas: Canvas, text: CharSequence?,
            start: Int, end: Int, x: Float,
            top: Int, y: Int, bottom: Int, paint: Paint
        ) {
            val d = getCachedDrawable() ?: return
            val rect = d.bounds
            canvas.save()
            val fontHeight = paint.fontMetrics.descent - paint.fontMetrics.ascent
            var transY = bottom - rect.bottom
            if (rect.height() < fontHeight) { // this is the fucking code which I waste 3 days
                when (mVerticalAlignment) {
                    ALIGN_BASELINE -> transY -= paint.fontMetricsInt.descent
                    ALIGN_CENTER -> transY = (transY - (fontHeight - rect.height()) / 2).toInt()
                    ALIGN_TOP -> transY = (transY - (fontHeight - rect.height())).toInt()
                }
            } else {
                if (mVerticalAlignment == ALIGN_BASELINE) {
                    transY -= paint.fontMetricsInt.descent
                }
            }
            canvas.translate(x, transY.toFloat())
            d.draw(canvas)
            canvas.restore()
        }

        private fun getCachedDrawable(): Drawable? {
            var d = mDrawableRef?.get()
            if (d == null) {
                d = getDrawable()
                mDrawableRef = WeakReference(d)
            }
            return d
        }

        private var mDrawableRef: WeakReference<Drawable?>? = null
    }
}