package com.common.weight.video

/**
 * 播放器状态的单一来源。UI 的加载框、错误面板、进度刷新任务与手势可用性
 * 全部由该状态驱动，避免多个布尔标记之间出现互相矛盾的中间态。
 */
enum class VideoPlayerState {
    /** 尚未设置数据源，或已重置 */
    IDLE,

    /** 正在准备或缓冲 */
    BUFFERING,

    /** 已就绪，可播放（含正在播放） */
    READY,

    /** 用户或系统暂停 */
    PAUSED,

    /** 播放完成 */
    ENDED,

    /** 播放失败，需要重试或退出 */
    ERROR,

    /** 播放器已释放，不再接受任何操作 */
    RELEASED,
}

/**
 * 暂停原因。决定「谁可以把播放恢复回来」：
 * 只有 [AUDIO_FOCUS_TRANSIENT] 允许引擎在重新获得焦点后自动续播，
 * 其余原因都必须由用户再次点击播放，或由页面回到前台后按用户意图恢复。
 */
enum class VideoPausedReason {
    /** 用户主动点击暂停 */
    USER,

    /** 页面进入后台 */
    LIFECYCLE,

    /** 临时失去音频焦点，重新获得后可自动续播 */
    AUDIO_FOCUS_TRANSIENT,

    /** 永久失去音频焦点，不得自动续播 */
    AUDIO_FOCUS_PERMANENT,

    /** 耳机拔出 / 蓝牙断开，不得自动续播 */
    BECOMING_NOISY,

    /** Surface 暂不可用，重建后按用户意图恢复 */
    SURFACE,

    /** 引擎内部暂停（缓冲等），不改变用户意图 */
    INTERNAL,
}

/**
 * 该暂停原因是否应清除用户播放意图（即必须由用户重新点击播放）。
 *
 * 引擎与 View 共用同一判定，避免两侧对「谁能自动恢复」出现分歧。
 */
fun VideoPausedReason.clearsPlayIntent(): Boolean = when (this) {
    VideoPausedReason.USER,
    VideoPausedReason.AUDIO_FOCUS_PERMANENT,
    VideoPausedReason.BECOMING_NOISY,
    -> true
    // 切后台与 Surface 不可用保留意图，条件满足后由宿主/引擎恢复；
    // 临时焦点丢失保留意图，等待 AUDIOFOCUS_GAIN 续播；
    // INTERNAL（缓冲）根本不改变意图
    VideoPausedReason.LIFECYCLE,
    VideoPausedReason.SURFACE,
    VideoPausedReason.AUDIO_FOCUS_TRANSIENT,
    VideoPausedReason.INTERNAL,
    -> false
}

/** 该暂停原因是否允许在重新获得音频焦点后自动续播 */
fun VideoPausedReason.allowsFocusGainResume(): Boolean =
    this == VideoPausedReason.AUDIO_FOCUS_TRANSIENT

/**
 * 该状态是否为「更高优先级的终止态」：延迟到达的 Ready / Surface / 焦点回调
 * 都不得把播放器从这些状态拉回播放。
 */
fun VideoPlayerState.isTerminal(): Boolean =
    this == VideoPlayerState.ENDED ||
        this == VideoPlayerState.ERROR ||
        this == VideoPlayerState.RELEASED

/**
 * 可区分的播放错误类型。UI 据此给出不同提示，
 * 并区分「致命错误（进入 [VideoPlayerState.ERROR]）」与「可恢复警告（继续播放）」。
 */
enum class VideoPlayerErrorType {
    /** 网络连接、超时、中断 */
    NETWORK,

    /** 地址无效、无权限、文件不存在、格式无法解析 */
    DATA_SOURCE,

    /** 找不到解码器或解码失败 */
    DECODER,

    /** Surface 不可用或渲染失败 */
    SURFACE,

    /** 音频输出（AudioTrack）异常 */
    AUDIO_OUTPUT,

    UNKNOWN,
}

/**
 * 带类型的播放异常。引擎的原始异常放在 [cause] 中，便于日志排查；
 * [errorType] 供 UI 选择提示文案，不要在提示中拼接可能含鉴权参数的完整地址。
 */
class VideoPlaybackException(
    val errorType: VideoPlayerErrorType,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
