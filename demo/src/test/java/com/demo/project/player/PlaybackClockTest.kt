package com.demo.project.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 统一媒体时钟与完成聚合的单元测试。
 *
 * 覆盖需求验收 6.1：音频先结束 / 视频先结束 / 同时结束 / 无音轨、
 * 非 seek 进度不回退、完成回调幂等、错误后 EOF 不得转为完成。
 */
class PlaybackClockTest {

    private fun clock(duration: Long = 10_000L) = PlaybackClock().apply { setDuration(duration) }

    private fun input(
        audio: Long,
        video: Long,
        wall: Long,
        drain: DrainSnapshot = DrainSnapshot(),
        speed: Float = 1f,
        terminated: Boolean = false,
    ) = ClockInput(
        audioOutputPositionMs = audio,
        videoPositionMs = video,
        speed = speed,
        wallClockMs = wall,
        drain = drain,
        terminated = terminated,
    )

    // ---------------- 主时钟选择 ----------------

    @Test
    fun `audio output position drives progress when audio is available`() {
        val c = clock()
        val out = c.update(input(audio = 1_000, video = 900, wall = 0))
        assertEquals(1_000L, out.positionMs)
        assertEquals(1_000L, out.masterClockMs)
    }

    @Test
    fun `no audio track uses video pts and publishes no master clock`() {
        val c = clock()
        val drain = DrainSnapshot(hasAudio = false)
        val out = c.update(input(audio = -1, video = 2_500, wall = 0, drain = drain))
        assertEquals(2_500L, out.positionMs)
        // 无音轨时不下发主时钟，由视频渲染层使用自身 PTS
        assertEquals(-1L, out.masterClockMs)
    }

    /** 这是「进度到结尾后回退到中间」的核心回归用例 */
    @Test
    fun `progress does not fall back to lagging video pts after audio ends`() {
        val c = clock(duration = 10_000L)
        // 音频推进到接近结尾
        c.update(input(audio = 9_800, video = 7_000, wall = 1_000))
        assertEquals(9_800L, c.positionMs)

        // 音频输出结束（位置不可用），视频仍落后在 7.0s —— 进度不得回退
        val out = c.update(input(audio = -1, video = 7_050, wall = 1_100))
        assertTrue(out.positionMs >= 9_800L)
        // 主时钟继续自由推进，而不是被撤下
        assertTrue(out.masterClockMs >= 9_800L)
    }

    @Test
    fun `free running clock advances with wall clock and speed after audio ends`() {
        val c = clock(duration = 60_000L)
        c.update(input(audio = 5_000, video = 5_000, wall = 0))
        // 音频结束后经过 1000ms 墙钟、2.0x 倍速 → 媒体时间应推进约 2000ms
        val out = c.update(input(audio = -1, video = 5_100, wall = 1_000, speed = 2f))
        assertEquals(7_000L, out.positionMs)
    }

    @Test
    fun `free running clock is capped at duration`() {
        val c = clock(duration = 10_000L)
        c.update(input(audio = 9_900, video = 9_000, wall = 0))
        val out = c.update(input(audio = -1, video = 9_100, wall = 10_000))
        assertEquals(10_000L, out.positionMs)
    }

    // ---------------- 进度单调性 ----------------

    @Test
    fun `progress is monotonic without seek`() {
        val c = clock()
        c.update(input(audio = 3_000, video = 3_000, wall = 0))
        // 迟到的、更小的采样不得让进度回退
        val out = c.update(input(audio = 2_400, video = 2_400, wall = 100))
        assertEquals(3_000L, out.positionMs)
    }

    @Test
    fun `seek allows progress to jump backwards`() {
        val c = clock()
        c.update(input(audio = 8_000, video = 8_000, wall = 0))
        c.beginSeek(2_000)
        assertEquals(2_000L, c.positionMs)
        // seek 期间只暴露目标位置，并把目标位置作为主时钟下发
        val during = c.update(input(audio = 7_900, video = 7_900, wall = 100))
        assertEquals(2_000L, during.positionMs)
        assertEquals(2_000L, during.masterClockMs)

        c.endSeek()
        val after = c.update(input(audio = 2_050, video = 2_050, wall = 200))
        assertEquals(2_050L, after.positionMs)
    }

    // ---------------- 完成聚合 ----------------

    @Test
    fun `completes when video and audio are fully drained`() {
        val c = clock()
        val drained = DrainSnapshot(
            videoDecoderDrained = true,
            hasAudio = true,
            audioDecoderDrained = true,
            audioOutputDrained = true,
        )
        assertTrue(c.update(input(audio = 10_000, video = 10_000, wall = 0, drain = drained)).shouldComplete)
    }

    /** 视频先结束：必须等待 AudioTrack 播完尾音，不得截断 */
    @Test
    fun `does not complete while audio output still draining`() {
        val c = clock()
        val drain = DrainSnapshot(
            videoDecoderDrained = true,
            hasAudio = true,
            audioDecoderDrained = true,
            audioOutputDrained = false,
        )
        assertFalse(c.update(input(audio = 9_500, video = 10_000, wall = 0, drain = drain)).shouldComplete)
    }

    /** 音频先结束：必须等待视频解码器排空 */
    @Test
    fun `does not complete while video decoder still draining`() {
        val c = clock()
        val drain = DrainSnapshot(
            videoDecoderDrained = false,
            hasAudio = true,
            audioDecoderDrained = true,
            audioOutputDrained = true,
        )
        assertFalse(c.update(input(audio = -1, video = 7_000, wall = 0, drain = drain)).shouldComplete)
    }

    @Test
    fun `does not complete while audio decoder or swr still draining`() {
        val c = clock()
        val drain = DrainSnapshot(
            videoDecoderDrained = true,
            hasAudio = true,
            audioDecoderDrained = false,
            audioOutputDrained = true,
        )
        assertFalse(c.update(input(audio = 9_900, video = 10_000, wall = 0, drain = drain)).shouldComplete)
    }

    @Test
    fun `no audio track completes on video drain alone`() {
        val c = clock()
        val drain = DrainSnapshot(videoDecoderDrained = true, hasAudio = false)
        assertTrue(c.update(input(audio = -1, video = 10_000, wall = 0, drain = drain)).shouldComplete)
    }

    @Test
    fun `error or stop prevents completion even when drained`() {
        val c = clock()
        val drain = DrainSnapshot(
            videoDecoderDrained = true,
            hasAudio = true,
            audioDecoderDrained = true,
            audioOutputDrained = true,
        )
        val out = c.update(input(audio = 10_000, video = 10_000, wall = 0, drain = drain, terminated = true))
        assertFalse(out.shouldComplete)
    }

    @Test
    fun `mark completed is idempotent and pins progress to duration`() {
        val c = clock(duration = 10_000L)
        c.update(input(audio = 9_800, video = 9_800, wall = 0))
        assertTrue(c.markCompleted())
        assertEquals(10_000L, c.positionMs)
        // 同一会话只能完成一次
        assertFalse(c.markCompleted())
    }

    @Test
    fun `completed session reports shouldComplete only once`() {
        val c = clock()
        val drain = DrainSnapshot(
            videoDecoderDrained = true,
            hasAudio = true,
            audioDecoderDrained = true,
            audioOutputDrained = true,
        )
        assertTrue(c.update(input(audio = 10_000, video = 10_000, wall = 0, drain = drain)).shouldComplete)
        c.markCompleted()
        assertFalse(c.update(input(audio = 10_000, video = 10_000, wall = 100, drain = drain)).shouldComplete)
    }

    @Test
    fun `reset starts a new session and clears completion`() {
        val c = clock()
        c.markCompleted()
        c.reset()
        assertFalse(c.completed)
        assertEquals(0L, c.positionMs)
        val drain = DrainSnapshot(videoDecoderDrained = true, hasAudio = false)
        assertTrue(c.update(input(audio = -1, video = 10_000, wall = 0, drain = drain)).shouldComplete)
    }

    @Test
    fun `seek after completion allows completing again`() {
        val c = clock()
        c.markCompleted()
        c.beginSeek(3_000)
        c.endSeek()
        assertFalse(c.completed)
    }
}
