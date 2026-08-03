## 需求：修复 FFmpeg 视频播放器音画不同步与进度不准问题

## 需求背景

### P1 高

- 严重等级：P1
- 文件路径：[ffmpeg_player_jni.cpp](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/lib_common-player/src/main/cpp/ffmpeg_player_jni.cpp:540)
- 代码位置：`sync_video_frame()`，540–542 行
- 问题描述：当视频帧 PTS 比音频主时钟超前超过 1 秒时，代码重建视频时钟并直接返回渲染该帧，而不是继续等待主时钟追上。
- 触发条件：合法变帧率视频存在超过 1 秒的相邻帧间隔（如静帧、低帧率片段或时间轴间隙），且音频仍在正常播放。
- 实际影响：未来画面会提前最多接近该 PTS 间隔显示，音画偏差可远超验收要求的 100ms，违反变帧率与 PTS 同步要求。
- 修复建议：主时钟有效时，不应以 `SYNC_MAX_WAIT_MS` 为由提前显示未来帧；保持分片等待并响应暂停/seek/stop。仅在主时钟失效或已确认时间轴重置时重建视频时钟。

- 严重等级：P1
- 文件路径：[FfmpegAudioPlayer.java](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/lib_common-player/src/main/java/com/common/player/FfmpegAudioPlayer.java:235)
- 代码位置：235 行、396–406 行
- 问题描述：`AudioTrack` 缓冲区未按目标倍速放大；`setPlaybackParams()` 因缓冲区不足失败时异常被静默吞掉，但 native 视频仍切换到新倍速。
- 触发条件：设备的 `minBufferSize` 大于约 250ms 内容长度时设为 `2.0x`（或更低阈值下的 `1.5x`），配置的缓冲区小于倍速所需的 `speed × minBufferSize`。
- 实际影响：音频可能维持原倍速，而视频按新倍速播放，造成持续且明显的音画不同步，无法满足 1.5x/2.0x 验收。Android 文档也明确指出流式 `AudioTrack` 在高倍速下需要足够大的缓冲区。[AudioTrack API](https://developer.android.com/reference/android/media/AudioTrack#setPlaybackParams(android.media.PlaybackParams))
- 修复建议：创建 `AudioTrack` 时按支持的最大倍速配置缓冲区（至少 `ceil(speed * minBufferSize)`），并在 `setPlaybackParams()` 失败时上报错误或阻止视频侧切速，不能静默继续。

### P2 中

- 严重等级：P2
- 文件路径：[CommonPlayerVideoEngine.kt](/Users/dian/Projects/AndroidStudioProjects/GitHubProjects/AndroidProject/demo/src/main/java/com/demo/project/player/CommonPlayerVideoEngine.kt:508)
- 代码位置：`finishPlaybackIfDrained()`，508–521 行
- 问题描述：视频解码完成后，音频尚未播放完仅等待 3 秒即无条件 `stopPlayers()`。
- 触发条件：有效媒体的音轨比视频轨长超过 3 秒。
- 实际影响：尾部音频被截断，不符合“以音视频实际结束状态判定完成”的要求。
- 修复建议：仅在确认音频输出无进展或发生错误时使用超时兜底；正常输出中的音频应等待 `isPlaybackFinished()`。


