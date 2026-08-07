package com.common.weight.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 暂停原因 → 恢复规则的状态转换测试。
 *
 * 这是引擎与 View 共用的唯一判定来源，覆盖需求中的关键回归点：
 * 用户暂停 / 永久焦点丢失后不得被焦点 Gain、Ready 或 Surface 回调自动恢复；
 * 切后台、临时焦点丢失、等待 Surface 必须保留播放意图。
 */
class VideoPausedReasonTest {

    @Test
    fun userPause_clearsIntent() {
        // 用户主动暂停后必须由用户再次点击播放
        assertTrue(VideoPausedReason.USER.clearsPlayIntent())
        assertFalse(VideoPausedReason.USER.allowsFocusGainResume())
    }

    @Test
    fun permanentFocusLoss_clearsIntentAndForbidsFocusResume() {
        // 永久丢失焦点：后续 AUDIOFOCUS_GAIN 不得自动播放
        assertTrue(VideoPausedReason.AUDIO_FOCUS_PERMANENT.clearsPlayIntent())
        assertFalse(VideoPausedReason.AUDIO_FOCUS_PERMANENT.allowsFocusGainResume())
    }

    @Test
    fun becomingNoisy_clearsIntentAndForbidsFocusResume() {
        // 耳机拔出 / 蓝牙断开：不得自动通过扬声器继续播放
        assertTrue(VideoPausedReason.BECOMING_NOISY.clearsPlayIntent())
        assertFalse(VideoPausedReason.BECOMING_NOISY.allowsFocusGainResume())
    }

    @Test
    fun transientFocusLoss_keepsIntentAndAllowsFocusResume() {
        // 临时丢失焦点是唯一允许自动续播的原因
        assertFalse(VideoPausedReason.AUDIO_FOCUS_TRANSIENT.clearsPlayIntent())
        assertTrue(VideoPausedReason.AUDIO_FOCUS_TRANSIENT.allowsFocusGainResume())
    }

    @Test
    fun lifecyclePause_keepsIntentButForbidsFocusResume() {
        // 切后台保留意图（返回前台按原意图恢复），但后台收到焦点 Gain 不得出声
        assertFalse(VideoPausedReason.LIFECYCLE.clearsPlayIntent())
        assertFalse(VideoPausedReason.LIFECYCLE.allowsFocusGainResume())
    }

    @Test
    fun surfacePause_keepsIntent() {
        // 等待 Surface 期间保留意图，Surface 重建后继续
        assertFalse(VideoPausedReason.SURFACE.clearsPlayIntent())
        assertFalse(VideoPausedReason.SURFACE.allowsFocusGainResume())
    }

    @Test
    fun internalPause_keepsIntent() {
        // 缓冲等内部暂停不改变用户意图
        assertFalse(VideoPausedReason.INTERNAL.clearsPlayIntent())
        assertFalse(VideoPausedReason.INTERNAL.allowsFocusGainResume())
    }

    @Test
    fun onlyTransientFocusLossAllowsAutoResume() {
        // 全量枚举校验：任何新增原因默认都不允许焦点自动续播，避免遗漏导致意外播放
        val allowed = VideoPausedReason.entries.filter { it.allowsFocusGainResume() }
        assertTrue(
            "只有 AUDIO_FOCUS_TRANSIENT 允许焦点恢复自动续播，实际=$allowed",
            allowed == listOf(VideoPausedReason.AUDIO_FOCUS_TRANSIENT)
        )
    }

    @Test
    fun terminalStates_rejectDelayedCallbacks() {
        // ENDED / ERROR / RELEASED 优先级最高，延迟回调不得把播放器拉回播放
        assertTrue(VideoPlayerState.ENDED.isTerminal())
        assertTrue(VideoPlayerState.ERROR.isTerminal())
        assertTrue(VideoPlayerState.RELEASED.isTerminal())
    }

    @Test
    fun nonTerminalStates_acceptResume() {
        assertFalse(VideoPlayerState.IDLE.isTerminal())
        assertFalse(VideoPlayerState.BUFFERING.isTerminal())
        assertFalse(VideoPlayerState.READY.isTerminal())
        assertFalse(VideoPlayerState.PAUSED.isTerminal())
    }
}
