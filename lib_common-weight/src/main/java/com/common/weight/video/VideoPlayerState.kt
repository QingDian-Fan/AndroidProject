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
