package com.demo.project.player

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 帧调度统计诊断测试。
 *
 * 覆盖需求验收 2.6 / 6.3：统计能够区分「均匀选帧」与「突发式连续丢帧」，
 * 并能识别设备解码能力不足。
 */
class FrameDropAnalyzerTest {

    @Test
    fun `no drops reports none`() {
        val d = FrameDropAnalyzer.analyze(
            decodedFrames = 600,
            renderedFrames = 600,
            maxConsecutiveDrops = 0,
            displayCapacityFrames = 600,
        )
        assertEquals(FrameDropVerdict.NONE, d.verdict)
        assertEquals(0.0, d.dropRatio, 0.0001)
    }

    /** 30fps 素材 3.0x、60Hz 屏幕：每秒 90 解码帧 / 60 展示帧，丢 1 显 2 交替 */
    @Test
    fun `even one in three selection is uniform`() {
        val d = FrameDropAnalyzer.analyze(
            decodedFrames = 900,
            renderedFrames = 600,
            maxConsecutiveDrops = 1,
            displayCapacityFrames = 600,
        )
        assertEquals(FrameDropVerdict.UNIFORM_SELECTION, d.verdict)
    }

    /** 每 3 个解码帧展示 1 帧：期望最大连续丢帧 3，实际 2 属于均匀 */
    @Test
    fun `one in three display ratio with small consecutive drops is uniform`() {
        val d = FrameDropAnalyzer.analyze(
            decodedFrames = 900,
            renderedFrames = 300,
            maxConsecutiveDrops = 2,
            displayCapacityFrames = 300,
        )
        assertEquals(FrameDropVerdict.UNIFORM_SELECTION, d.verdict)
        assertEquals(3L, d.expectedMaxConsecutiveDrops)
    }

    /** 这是修复前「连丢 15 帧、只显示 1 帧」的回归用例 */
    @Test
    fun `burst dropping is detected`() {
        val d = FrameDropAnalyzer.analyze(
            decodedFrames = 900,
            renderedFrames = 600,
            maxConsecutiveDrops = 15,
            displayCapacityFrames = 600,
        )
        assertEquals(FrameDropVerdict.BURST_DROP, d.verdict)
    }

    @Test
    fun `burst dropping detected even when drop ratio is low`() {
        val d = FrameDropAnalyzer.analyze(
            decodedFrames = 1000,
            renderedFrames = 950,
            maxConsecutiveDrops = 20,
            displayCapacityFrames = 1000,
        )
        assertEquals(FrameDropVerdict.BURST_DROP, d.verdict)
    }

    /** 丢帧均匀但展示帧数远低于屏幕能力 → 设备解码跟不上 */
    @Test
    fun `slow device is distinguished from uniform selection`() {
        val d = FrameDropAnalyzer.analyze(
            decodedFrames = 600,
            renderedFrames = 200,
            maxConsecutiveDrops = 3,
            displayCapacityFrames = 600,
        )
        assertEquals(FrameDropVerdict.DEVICE_TOO_SLOW, d.verdict)
    }

    @Test
    fun `zero decoded frames is none`() {
        val d = FrameDropAnalyzer.analyze(0, 0, 0, 600)
        assertEquals(FrameDropVerdict.NONE, d.verdict)
    }

    @Test
    fun `drop ratio is computed from decoded and rendered frames`() {
        val d = FrameDropAnalyzer.analyze(
            decodedFrames = 1000,
            renderedFrames = 750,
            maxConsecutiveDrops = 1,
            displayCapacityFrames = 1000,
        )
        assertEquals(0.25, d.dropRatio, 0.0001)
    }
}
