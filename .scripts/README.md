## 需求：修复 FFmpeg 视频播放器音画不同步与进度不准问题

## 需求背景

### P1 高

- 严重等级：P1
- 文件路径：`demo/src/main/java/com/demo/project/player/CommonPlayerVideoEngine.kt`
- 代码位置：293–299 行
- 问题描述：运行中切换倍速时，视频先切到目标倍速，再调用音频切速。若 `AudioTrack.setPlaybackParams()` 失败，`FfmpegAudioPlayer` 仅回退音频并记录日志，不向调用方报告失败；视频仍保持新倍速。
- 触发条件：已建立 `AudioTrack` 后，设备拒绝目标 `PlaybackParams`（例如设备不支持该速度或轨道状态异常）。
- 实际影响：音频继续以旧倍速输出，视频以新倍速渲染，造成持续音画不同步。新增的 `getPlaybackSpeed()` 未被该调用链使用，未满足“失败时上报错误或阻止视频侧切速”的要求。
- 修复建议：让 `FfmpegAudioPlayer.setPlaybackSpeed()` 返回成功状态或抛出可处理异常；`CommonPlayerVideoEngine` 应先确认音频切速成功，再切换视频；失败时回退/保持视频原速度并上报播放错误。

### P2 中

- 严重等级：P2
- 文件路径：`demo/src/main/java/com/demo/project/player/CommonPlayerVideoEngine.kt`
- 代码位置：520–560 行
- 问题描述：音频排空期间，用户主动暂停会被当作“音频输出停滞”。时钟轮询未在 `paused` 状态停止，3 秒后 `isAudioOutputStalled()` 返回 true，随后无条件 `stopPlayers()` 并上报结束。
- 触发条件：视频流先结束、音频仍有尾段；用户在尾音播放期间执行暂停，或 Activity `onPause()` / Surface 销毁触发暂停，并停留超过 3 秒。
- 实际影响：正常的暂停/恢复语义被破坏，尾部音频被停止，恢复时播放器已进入 ended 状态，无法继续播放剩余尾音。
- 修复建议：在 `paused`、Surface 重建等待或音频焦点导致的预期暂停状态下跳过停滞超时判断；恢复后重新开始停滞计时。仅在播放应持续进行且输出确实无进展时启用兜底。


