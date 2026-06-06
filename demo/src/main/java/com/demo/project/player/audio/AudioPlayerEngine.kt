package com.demo.project.player.audio

/**
 * 音频播放引擎抽象。
 *
 * 上层 UI 只依赖该接口，不关心底层用 ExoPlayer 还是 FFmpeg 等具体实现，
 * 具体实现由 [AudioPlayerEngineFactory] 通过工厂模式创建，便于拓展与切换。
 */
interface AudioPlayerEngine {

    /** 是否正在播放 */
    val isPlaying: Boolean

    /** 是否已播放到末尾 */
    val isEnded: Boolean

    /** 总时长（毫秒），未知时返回 0 */
    val duration: Long

    /** 当前播放位置（毫秒） */
    val currentPosition: Long

    /** 已缓冲位置（毫秒） */
    val bufferedPosition: Long

    /** 设置状态回调，传 null 解除 */
    fun setListener(listener: Listener?)

    /** 设置播放地址（http(s)/file/content 均可） */
    fun setDataSource(urlString: String)

    /** 设置倍速 */
    fun setPlaybackSpeed(speed: Float)

    /** 准备播放（异步），就绪后回调 [Listener.onReady] */
    fun prepare()

    /** 开始/恢复播放 */
    fun play()

    /** 暂停 */
    fun pause()

    /** 跳转到指定位置（毫秒） */
    fun seekTo(positionMs: Long)

    /** 释放资源，释放后实例不可再用 */
    fun release()

    interface Listener {

        /** 正在缓冲 */
        fun onBuffering()

        /** 已就绪，可获取 [duration] 等信息 */
        fun onReady()

        /** 播放/暂停状态变化 */
        fun onPlayingChanged(isPlaying: Boolean)

        /** 播放结束 */
        fun onEnded()

        /** 出错 */
        fun onError(error: Throwable)
    }
}
