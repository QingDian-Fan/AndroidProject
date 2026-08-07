package com.common.weight.video

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 播放状态转换规则测试。
 *
 * 覆盖本次修复的两个 P1 回归点：
 *  1) 准备/缓冲期间暂停后，滞后到达的 Ready 回调不得把状态覆盖成 READY（假播放 + 多余进度刷新）；
 *  2) 用户暂停 / 永久焦点丢失后，引擎的自动续播不得把播放器拉回播放。
 *
 * 这里测的是 [VideoPlayerView] 实际调用的同一批判定函数（`resolveReadyState` /
 * `shouldAcceptAutoResume`），因此规则变化会被直接捕获；View 的控件副作用需真机验证。
 */
class VideoPlayerStateTransitionTest {

    // ---------- Ready 回调 ----------

    @Test
    fun ready_whileBuffering_withPlayIntent_entersReady() {
        // 正常路径：准备完成且用户期望播放 → 进入 READY
        assertEquals(
            VideoPlayerState.READY,
            resolveReadyState(VideoPlayerState.BUFFERING, pauseReason = null, playIntended = true)
        )
    }

    @Test
    fun ready_afterUserPauseDuringBuffering_staysPaused() {
        // P1 回归点：准备中用户暂停，随后 Ready 到达 → 必须保持 PAUSED
        assertEquals(
            VideoPlayerState.PAUSED,
            resolveReadyState(
                VideoPlayerState.BUFFERING,
                pauseReason = VideoPausedReason.USER,
                playIntended = false
            )
        )
    }

    @Test
    fun ready_afterLifecyclePauseDuringBuffering_staysPaused() {
        // 切后台保留播放意图，但 Ready 仍不得在后台把状态拉成播放
        assertEquals(
            VideoPlayerState.PAUSED,
            resolveReadyState(
                VideoPlayerState.BUFFERING,
                pauseReason = VideoPausedReason.LIFECYCLE,
                playIntended = true
            )
        )
    }

    @Test
    fun ready_afterTransientFocusLoss_staysPaused() {
        assertEquals(
            VideoPlayerState.PAUSED,
            resolveReadyState(
                VideoPlayerState.BUFFERING,
                pauseReason = VideoPausedReason.AUDIO_FOCUS_TRANSIENT,
                playIntended = true
            )
        )
    }

    @Test
    fun ready_withoutPlayIntent_staysPaused() {
        // 没有暂停原因但也没有播放意图（例如首帧就绪但用户从未点击播放）
        assertEquals(
            VideoPlayerState.PAUSED,
            resolveReadyState(VideoPlayerState.BUFFERING, pauseReason = null, playIntended = false)
        )
    }

    @Test
    fun ready_inTerminalStates_isIgnored() {
        // 延迟到达的 Ready 不得把播放器从 ERROR / ENDED / RELEASED 拉回
        listOf(VideoPlayerState.ERROR, VideoPlayerState.ENDED, VideoPlayerState.RELEASED)
            .forEach { state ->
                assertNull(
                    "state=$state 时 Ready 必须被忽略",
                    resolveReadyState(state, pauseReason = null, playIntended = true)
                )
            }
    }

    // ---------- 引擎自动续播 ----------

    @Test
    fun autoResume_afterTransientFocusLoss_isAccepted() {
        // 临时焦点恢复是唯一允许自动续播的场景
        assertTrue(
            shouldAcceptAutoResume(
                VideoPlayerState.PAUSED,
                VideoPausedReason.AUDIO_FOCUS_TRANSIENT
            )
        )
    }

    @Test
    fun autoResume_afterUserPause_isRejected() {
        // P1 回归点：用户暂停后收到焦点 Gain，不得自动播放
        assertFalse(shouldAcceptAutoResume(VideoPlayerState.PAUSED, VideoPausedReason.USER))
    }

    @Test
    fun autoResume_afterPermanentFocusLoss_isRejected() {
        assertFalse(
            shouldAcceptAutoResume(
                VideoPlayerState.PAUSED,
                VideoPausedReason.AUDIO_FOCUS_PERMANENT
            )
        )
    }

    @Test
    fun autoResume_afterBecomingNoisy_isRejected() {
        assertFalse(
            shouldAcceptAutoResume(VideoPlayerState.PAUSED, VideoPausedReason.BECOMING_NOISY)
        )
    }

    @Test
    fun autoResume_inTerminalStates_isRejected() {
        listOf(VideoPlayerState.ERROR, VideoPlayerState.ENDED, VideoPlayerState.RELEASED)
            .forEach { state ->
                assertFalse(
                    "state=$state 时不得自动续播",
                    shouldAcceptAutoResume(state, VideoPausedReason.AUDIO_FOCUS_TRANSIENT)
                )
            }
    }

    @Test
    fun autoResume_whenNotPaused_isAccepted() {
        // 没有生效的暂停原因时（例如缓冲结束），续播请求可以被接受
        assertTrue(shouldAcceptAutoResume(VideoPlayerState.PAUSED, null))
    }
}
