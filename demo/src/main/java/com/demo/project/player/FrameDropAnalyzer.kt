package com.demo.project.player

/**
 * 帧调度诊断结论。用于把「必要的均匀选帧」和「播放器调度异常」区分开，
 * 避免把设备性能不足或屏幕刷新限制误判成播放器 bug（反之亦然）。
 */
enum class FrameDropVerdict {
    /** 没有丢帧 */
    NONE,

    /** 均匀选帧：媒体帧率超过屏幕刷新能力时的正常行为 */
    UNIFORM_SELECTION,

    /** 成组丢帧：连续丢弃远超均匀选帧应有的幅度，属于调度异常 */
    BURST_DROP,

    /** 设备解码能力不足：展示帧率显著低于屏幕刷新能力 */
    DEVICE_TOO_SLOW,
}

data class FrameDropDiagnosis(
    val verdict: FrameDropVerdict,
    /** 丢帧率 [0,1] */
    val dropRatio: Double,
    /** 均匀选帧时期望的最大连续丢帧数 */
    val expectedMaxConsecutiveDrops: Long,
)

/**
 * 依据帧调度统计判断丢帧模式。
 *
 * 均匀选帧的判定依据：若每 N 帧展示 1 帧，则连续丢帧数不应超过 N-1（允许 1 帧误差）。
 * 例如 30fps 素材 3.0x 播放、60Hz 屏幕：每秒 90 个媒体帧、最多展示 60 帧，
 * 期望「丢 1 显 1」交替，最大连续丢帧应为 1；若统计到连续丢 15 帧，即为成组丢帧。
 *
 * 纯函数，无 Android 依赖，可独立单元测试。
 */
object FrameDropAnalyzer {

    /** 展示帧率低于屏幕刷新能力该比例时，判定为设备解码能力不足 */
    private const val SLOW_DEVICE_RATIO = 0.5

    /**
     * @param decodedFrames        已解码帧数
     * @param renderedFrames       已真实展示帧数
     * @param maxConsecutiveDrops  统计到的最大连续丢帧数
     * @param displayCapacityFps   屏幕在本次播放期间可展示的帧数上限（刷新率 × 时长）
     */
    fun analyze(
        decodedFrames: Long,
        renderedFrames: Long,
        maxConsecutiveDrops: Long,
        displayCapacityFrames: Long,
    ): FrameDropDiagnosis {
        if (decodedFrames <= 0L) {
            return FrameDropDiagnosis(FrameDropVerdict.NONE, 0.0, 0L)
        }
        val dropped = (decodedFrames - renderedFrames).coerceAtLeast(0L)
        val dropRatio = dropped.toDouble() / decodedFrames.toDouble()

        if (dropped == 0L) {
            return FrameDropDiagnosis(FrameDropVerdict.NONE, 0.0, 0L)
        }

        // 每展示 1 帧平均对应多少个解码帧 → 期望的最大连续丢帧数
        val framesPerDisplayed = if (renderedFrames > 0L) {
            decodedFrames.toDouble() / renderedFrames.toDouble()
        } else {
            decodedFrames.toDouble()
        }
        // 允许 1 帧误差，避免边界抖动被误判
        val expectedMax = Math.ceil(framesPerDisplayed).toLong().coerceAtLeast(1L)
        val tolerated = expectedMax + 1

        if (maxConsecutiveDrops > tolerated) {
            return FrameDropDiagnosis(FrameDropVerdict.BURST_DROP, dropRatio, expectedMax)
        }

        // 丢帧均匀，但展示帧数远低于屏幕能力 → 是设备解码跟不上，而不是主动选帧
        if (displayCapacityFrames > 0L &&
            renderedFrames < displayCapacityFrames * SLOW_DEVICE_RATIO
        ) {
            return FrameDropDiagnosis(FrameDropVerdict.DEVICE_TOO_SLOW, dropRatio, expectedMax)
        }

        return FrameDropDiagnosis(FrameDropVerdict.UNIFORM_SELECTION, dropRatio, expectedMax)
    }
}
