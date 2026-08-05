package com.common.image.engine

import com.common.image.ImageScaleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 覆盖「缩放模式 + 圆角」在不同宽高比下的输出尺寸。
 *
 * 回归点：Coil 自带的 RoundedCornersTransformation 固定按 Scale.FILL 裁剪，
 * 导致 FIT_CENTER / CENTER_INSIDE 组合圆角时图片被裁掉。
 */
class CoilRoundedCornersTransformationTest {

    private companion object {
        const val DST = 400
        const val DELTA = 0.0001f
    }

    /** 宽高比是否与源图一致，即完全没有裁剪 */
    private fun assertNoCrop(srcWidth: Int, srcHeight: Int, output: RoundedCornersOutput) {
        val srcRatio = srcWidth.toDouble() / srcHeight
        val outRatio = output.outputWidth.toDouble() / output.outputHeight
        assertTrue(
            "expected no crop, src ratio=$srcRatio, output ratio=$outRatio",
            kotlin.math.abs(srcRatio - outRatio) < 0.01
        )
    }

    @Test
    fun fitCenter_wideImage_keepsFullImage() {
        val output = computeRoundedCornersOutput(800, 400, DST, DST, ImageScaleType.FIT_CENTER)

        assertEquals(400, output.outputWidth)
        assertEquals(200, output.outputHeight)
        assertEquals(0.5f, output.multiplier, DELTA)
        assertNoCrop(800, 400, output)
    }

    @Test
    fun fitCenter_tallImage_keepsFullImage() {
        val output = computeRoundedCornersOutput(400, 800, DST, DST, ImageScaleType.FIT_CENTER)

        assertEquals(200, output.outputWidth)
        assertEquals(400, output.outputHeight)
        assertEquals(0.5f, output.multiplier, DELTA)
        assertNoCrop(400, 800, output)
    }

    @Test
    fun fitCenter_smallImage_scalesUpWithoutCrop() {
        val output = computeRoundedCornersOutput(100, 50, DST, DST, ImageScaleType.FIT_CENTER)

        assertEquals(400, output.outputWidth)
        assertEquals(200, output.outputHeight)
        assertEquals(4f, output.multiplier, DELTA)
        assertNoCrop(100, 50, output)
    }

    @Test
    fun centerInside_smallImage_isNotScaledUp() {
        val output = computeRoundedCornersOutput(100, 50, DST, DST, ImageScaleType.CENTER_INSIDE)

        assertEquals(100, output.outputWidth)
        assertEquals(50, output.outputHeight)
        assertEquals(1f, output.multiplier, DELTA)
        assertNoCrop(100, 50, output)
    }

    @Test
    fun centerInside_largeImage_shrinksWithoutCrop() {
        val output = computeRoundedCornersOutput(800, 400, DST, DST, ImageScaleType.CENTER_INSIDE)

        assertEquals(400, output.outputWidth)
        assertEquals(200, output.outputHeight)
        assertEquals(0.5f, output.multiplier, DELTA)
        assertNoCrop(800, 400, output)
    }

    @Test
    fun centerCrop_wideImage_fillsTargetAndCrops() {
        val output = computeRoundedCornersOutput(800, 400, DST, DST, ImageScaleType.CENTER_CROP)

        // CENTER_CROP 语义就是填满目标区域并裁掉多余部分
        assertEquals(DST, output.outputWidth)
        assertEquals(DST, output.outputHeight)
        assertEquals(1f, output.multiplier, DELTA)
    }

    @Test
    fun centerCrop_tallImage_fillsTarget() {
        val output = computeRoundedCornersOutput(400, 800, DST, DST, ImageScaleType.CENTER_CROP)

        assertEquals(DST, output.outputWidth)
        assertEquals(DST, output.outputHeight)
        assertEquals(1f, output.multiplier, DELTA)
    }

    @Test
    fun none_keepsOriginalSize() {
        val output = computeRoundedCornersOutput(800, 400, DST, DST, ImageScaleType.NONE)

        assertEquals(800, output.outputWidth)
        assertEquals(400, output.outputHeight)
        assertEquals(1f, output.multiplier, DELTA)
        assertNoCrop(800, 400, output)
    }

    @Test
    fun nonSquareTarget_fitCenter_keepsFullImage() {
        val output = computeRoundedCornersOutput(1000, 250, 300, 600, ImageScaleType.FIT_CENTER)

        // 受宽度限制：1000 -> 300，高度等比缩到 75
        assertEquals(300, output.outputWidth)
        assertEquals(75, output.outputHeight)
        assertNoCrop(1000, 250, output)
    }

    @Test
    fun undefinedTargetSize_keepsOriginalSize() {
        ImageScaleType.entries.forEach { scaleType ->
            val output = computeRoundedCornersOutput(800, 400, 0, 0, scaleType)

            assertEquals("scaleType=$scaleType", 800, output.outputWidth)
            assertEquals("scaleType=$scaleType", 400, output.outputHeight)
            assertEquals("scaleType=$scaleType", 1f, output.multiplier, DELTA)
        }
    }

    @Test
    fun invalidSourceSize_doesNotProduceEmptyBitmap() {
        ImageScaleType.entries.forEach { scaleType ->
            val output = computeRoundedCornersOutput(0, 0, DST, DST, scaleType)

            assertTrue("scaleType=$scaleType", output.outputWidth >= 1)
            assertTrue("scaleType=$scaleType", output.outputHeight >= 1)
        }
    }

    @Test
    fun extremeAspectRatio_stillProducesValidSize() {
        val output = computeRoundedCornersOutput(10000, 1, DST, DST, ImageScaleType.FIT_CENTER)

        // 高度按比例会趋近 0，必须兜底为 1，避免 createBitmap 抛 IllegalArgumentException
        assertEquals(400, output.outputWidth)
        assertTrue(output.outputHeight >= 1)
    }
}
