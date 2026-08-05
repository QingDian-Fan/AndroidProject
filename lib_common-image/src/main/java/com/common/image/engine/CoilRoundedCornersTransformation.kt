package com.common.image.engine

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import coil.size.Size
import coil.size.pxOrElse
import coil.transform.Transformation
import com.common.image.ImageScaleType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 遵循 [ImageScaleType] 的圆角变换。
 *
 * Coil 2.x 自带的 `RoundedCornersTransformation` 内部固定按 `Scale.FILL` 计算，
 * 并把输出尺寸拉伸到目标区域，因此 `FIT_CENTER` / `CENTER_INSIDE` 搭配圆角时图片会被居中裁剪，
 * 与 Glide 的 `FitCenter + RoundedCorners` 行为不一致。这里按缩放模式分别计算缩放系数与输出尺寸：
 *
 * - [ImageScaleType.CENTER_CROP]：输出等于目标区域，超出部分居中裁掉；
 * - [ImageScaleType.FIT_CENTER]：输出等于等比缩放后的图片尺寸，完整显示不裁剪；
 * - [ImageScaleType.CENTER_INSIDE]：同 FIT_CENTER，但不放大小图；
 * - [ImageScaleType.NONE]：保持原始尺寸，仅做圆角。
 *
 * 输出尺寸恰好等于图片可见区域，因此圆角画在真实边界上；后续 ImageView 按同样的 scaleType
 * 展示时缩放系数为 1，圆角半径不会被二次拉伸。
 */
internal class CoilRoundedCornersTransformation(
    private val radiusPx: Int,
    private val scaleType: ImageScaleType
) : Transformation {

    override val cacheKey: String =
        "${CoilRoundedCornersTransformation::class.java.name}-$radiusPx-$scaleType"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Size 可能为 ORIGINAL（宽高均未定义），此时退化为「不缩放」
        val dstWidth = size.width.pxOrElse { input.width }
        val dstHeight = size.height.pxOrElse { input.height }
        val output = computeRoundedCornersOutput(
            srcWidth = input.width,
            srcHeight = input.height,
            dstWidth = dstWidth,
            dstHeight = dstHeight,
            scaleType = scaleType
        )

        // 圆角需要透明像素，固定使用 ARGB_8888，避免 RGB_565 等不透明配置画不出圆角
        val result = Bitmap.createBitmap(
            output.outputWidth,
            output.outputHeight,
            Bitmap.Config.ARGB_8888
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val shader = BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val matrix = Matrix().apply {
            // 居中：CENTER_CROP 时为负值（裁掉两侧），其余模式约等于 0
            setTranslate(
                (output.outputWidth - output.multiplier * input.width) / 2f,
                (output.outputHeight - output.multiplier * input.height) / 2f
            )
            preScale(output.multiplier, output.multiplier)
        }
        shader.setLocalMatrix(matrix)
        paint.shader = shader

        val radius = radiusPx.toFloat()
        Canvas(result).drawRoundRect(
            0f,
            0f,
            output.outputWidth.toFloat(),
            output.outputHeight.toFloat(),
            radius,
            radius,
            paint
        )
        // 及时断开对 input 的引用，输入位图由 Coil 负责回收
        paint.shader = null
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is CoilRoundedCornersTransformation &&
                radiusPx == other.radiusPx &&
                scaleType == other.scaleType
    }

    override fun hashCode(): Int = 31 * radiusPx + scaleType.hashCode()
}

/** [computeRoundedCornersOutput] 的计算结果：输出位图尺寸与源图缩放系数。 */
internal data class RoundedCornersOutput(
    val outputWidth: Int,
    val outputHeight: Int,
    val multiplier: Float
)

/**
 * 按 [scaleType] 计算圆角变换的输出尺寸与缩放系数。
 *
 * 抽成纯函数以便在 JVM 单测中覆盖各种宽高比组合（见 CoilRoundedCornersTransformationTest）。
 */
internal fun computeRoundedCornersOutput(
    srcWidth: Int,
    srcHeight: Int,
    dstWidth: Int,
    dstHeight: Int,
    scaleType: ImageScaleType
): RoundedCornersOutput {
    // 源图尺寸非法时不做任何缩放，交由上层的错误分支处理
    if (srcWidth <= 0 || srcHeight <= 0) {
        return RoundedCornersOutput(max(srcWidth, 1), max(srcHeight, 1), 1f)
    }
    // 目标尺寸不可用（未测量、ORIGINAL 等）时保持原图尺寸
    if (dstWidth <= 0 || dstHeight <= 0) {
        return RoundedCornersOutput(srcWidth, srcHeight, 1f)
    }

    val widthRatio = dstWidth.toDouble() / srcWidth
    val heightRatio = dstHeight.toDouble() / srcHeight
    val multiplier = when (scaleType) {
        ImageScaleType.CENTER_CROP -> max(widthRatio, heightRatio)
        ImageScaleType.FIT_CENTER -> min(widthRatio, heightRatio)
        // CENTER_INSIDE 只缩小不放大
        ImageScaleType.CENTER_INSIDE -> min(1.0, min(widthRatio, heightRatio))
        ImageScaleType.NONE -> 1.0
    }

    return if (scaleType == ImageScaleType.CENTER_CROP) {
        // 填满目标区域，超出部分裁掉
        RoundedCornersOutput(dstWidth, dstHeight, multiplier.toFloat())
    } else {
        // 输出即缩放后的完整图片，不产生裁剪
        RoundedCornersOutput(
            (srcWidth * multiplier).roundToInt().coerceAtLeast(1),
            (srcHeight * multiplier).roundToInt().coerceAtLeast(1),
            multiplier.toFloat()
        )
    }
}
