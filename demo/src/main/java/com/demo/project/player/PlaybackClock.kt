package com.demo.project.player

/**
 * 各解码 / 输出环节的排空快照。每个字段都来自唯一权威层，本类只消费不修改：
 *
 * - [videoDecoderDrained]：Native 视频解码层（解码器已返回 EOF）
 * - [audioDecoderDrained]：Native 音频解码层（解码器 + SWR 均已排空）
 * - [audioOutputDrained]：音频输出层（AudioTrack 播放头 >= 已写入帧数）
 */
data class DrainSnapshot(
    val videoDecoderDrained: Boolean = false,
    val hasAudio: Boolean = true,
    val audioDecoderDrained: Boolean = false,
    val audioOutputDrained: Boolean = false,
)

/**
 * 一次时钟采样的输入。位置为负表示该来源当前不可用。
 *
 * @param audioOutputPositionMs AudioTrack **实际播放头**换算出的媒体时间，
 *        不得传入已解码或已写入但尚未播放的 PCM 位置
 * @param videoPositionMs       视频当前有效 PTS
 * @param speed                 当前**实际生效**倍速（音频已确认接受）
 * @param wallClockMs           单调墙钟（SystemClock.uptimeMillis）
 */
data class ClockInput(
    val audioOutputPositionMs: Long,
    val videoPositionMs: Long,
    val speed: Float,
    val wallClockMs: Long,
    val drain: DrainSnapshot,
    /** 已上报致命错误 / 已停止 / 正在换源：此时禁止判定完成 */
    val terminated: Boolean = false,
)

/**
 * 一次时钟采样的结果。
 *
 * @param positionMs   对外公开进度
 * @param masterClockMs 应下发给视频渲染层的主时钟；-1 表示无主时钟，
 *        视频退化为自身 PTS 时钟（仅无音轨媒体会出现）
 * @param shouldComplete 是否满足最终完成条件
 */
data class ClockOutput(
    val positionMs: Long,
    val masterClockMs: Long,
    val shouldComplete: Boolean,
)

/**
 * 唯一逻辑媒体时钟、公开进度与完成聚合。
 *
 * 纯 Kotlin、无 Android 依赖、无内部计时器，全部输入由调用方以快照传入，
 * 因此可以被独立单元测试覆盖（音频先结束 / 视频先结束 / 无音轨 / seek / 完成幂等）。
 *
 * 关键约束：
 * 1. 除用户主动 seek 外，[positionMs] 单调不减。
 * 2. 音频输出结束后**不会**把进度切到更小的视频 PTS：主时钟从音频最后的有效位置
 *    按墙钟与实际倍速自由推进，视频据此追赶或均匀跳过过期帧。
 * 3. 只有视频解码器排空、且（无音轨 或 音频解码器+SWR 排空且 AudioTrack 播完）
 *    且未处于 seek / 错误 / 停止时，才判定完成。
 * 4. [markCompleted] 幂等，同一会话只会返回一次 true。
 */
class PlaybackClock {

    /** 对外公开进度（毫秒） */
    var positionMs: Long = 0L
        private set

    /** 总时长，<=0 表示未知 */
    var durationMs: Long = 0L
        private set

    /** 本次会话是否已上报完成 */
    var completed: Boolean = false
        private set

    /** 是否正在等待 seek 落点 */
    private var seeking = false

    /** 音频主时钟是否曾经有效：决定音频结束后是自由推进还是使用视频自身时钟 */
    private var audioMasterEstablished = false

    /** 音频主时钟失效后的自由推进锚点 */
    private var freeRunMediaMs = -1L
    private var freeRunWallMs = 0L

    /**
     * 复位到新一轮播放（换源、重试、重新起播）。
     * 会清除完成标记与全部时钟锚点，允许进度重新从 [startPositionMs] 开始。
     */
    fun reset(startPositionMs: Long = 0L) {
        positionMs = startPositionMs.coerceAtLeast(0L)
        completed = false
        seeking = false
        audioMasterEstablished = false
        freeRunMediaMs = -1L
        freeRunWallMs = 0L
    }

    fun setDuration(duration: Long) {
        if (duration > 0L) {
            durationMs = duration
        }
    }

    /**
     * 用户发起 seek：允许进度直接跳到目标位置（含向前和向后）。
     * seek 期间对外只暴露目标位置，旧位置的迟到采样不得污染进度。
     */
    fun beginSeek(targetMs: Long) {
        seeking = true
        positionMs = clamp(targetMs)
        // seek 落点会重建全部时钟，旧锚点必须失效
        audioMasterEstablished = false
        freeRunMediaMs = -1L
        freeRunWallMs = 0L
        // 结束后回拖属于重新播放，允许再次判定完成
        completed = false
    }

    /** seek 落点已生效（或超时放弃等待），恢复正常采样 */
    fun endSeek() {
        seeking = false
    }

    val isSeeking: Boolean get() = seeking

    /**
     * 消费一次采样，返回公开进度、应下发的主时钟与是否满足完成条件。
     *
     * seek 期间只回放目标位置；非 seek 期间进度单调不减。
     */
    fun update(input: ClockInput): ClockOutput {
        if (seeking) {
            // 等待落点：对外保持目标位置，同时把目标位置作为主时钟下发，
            // 让视频朝目标位置调度，而不是继续渲染旧位置的帧
            return ClockOutput(positionMs, positionMs, shouldComplete = false)
        }

        val master = resolveMasterClock(input)
        // 非 seek 场景进度不得回退：这里的单调性来自「主时钟本身单调」，
        // 而不是无条件 maxOf —— 音频结束后主时钟继续自由推进，不会退回视频 PTS
        val next = clamp(master.mediaMs)
        if (next > positionMs) {
            positionMs = next
        }

        val shouldComplete = !completed && !input.terminated && isDrained(input.drain)
        return ClockOutput(positionMs, master.publishMs, shouldComplete)
    }

    /**
     * 标记完成。幂等：同一会话只会返回一次 true，重复调用返回 false。
     * 完成时才允许把公开进度推到总时长。
     */
    fun markCompleted(): Boolean {
        if (completed) {
            return false
        }
        completed = true
        if (durationMs > 0L) {
            positionMs = durationMs
        }
        return true
    }

    /** 最终完成条件：视频解码器排空 + 音频侧全部排空（或无音轨） */
    private fun isDrained(drain: DrainSnapshot): Boolean {
        if (!drain.videoDecoderDrained) {
            return false
        }
        if (!drain.hasAudio) {
            return true
        }
        return drain.audioDecoderDrained && drain.audioOutputDrained
    }

    private class MasterClock(val mediaMs: Long, val publishMs: Long)

    /**
     * 主时钟选择：
     *
     * 1. 音频输出可用 → 直接使用 AudioTrack 实际播放头位置，并记录自由推进锚点。
     * 2. 音频输出曾经可用但已结束/失效 → 从最后的有效媒体时间按墙钟 × 实际倍速自由推进。
     *    **不回退到更小的视频 PTS**，视频据此追赶或均匀跳过过期帧。
     * 3. 从未有过音频主时钟（无音轨媒体）→ 使用视频自身 PTS，且不下发主时钟（-1），
     *    由视频渲染层用自己的时钟推进。
     */
    private fun resolveMasterClock(input: ClockInput): MasterClock {
        val audioPos = input.audioOutputPositionMs
        if (input.drain.hasAudio && audioPos >= 0L) {
            audioMasterEstablished = true
            freeRunMediaMs = audioPos
            freeRunWallMs = input.wallClockMs
            return MasterClock(audioPos, audioPos)
        }

        if (audioMasterEstablished && freeRunMediaMs >= 0L) {
            val speed = if (input.speed > 0f) input.speed else 1f
            val elapsed = (input.wallClockMs - freeRunWallMs).coerceAtLeast(0L)
            val advanced = freeRunMediaMs + (elapsed * speed).toLong()
            // 自由推进不越过总时长，避免尾段进度冲过 100%
            val capped = if (durationMs > 0L) advanced.coerceAtMost(durationMs) else advanced
            return MasterClock(capped, capped)
        }

        // 无音轨：视频自身 PTS 就是唯一时钟，不下发主时钟
        val videoPos = if (input.videoPositionMs >= 0L) input.videoPositionMs else positionMs
        return MasterClock(videoPos, -1L)
    }

    private fun clamp(value: Long): Long {
        val safe = value.coerceAtLeast(0L)
        return if (durationMs > 0L) safe.coerceAtMost(durationMs) else safe
    }
}
