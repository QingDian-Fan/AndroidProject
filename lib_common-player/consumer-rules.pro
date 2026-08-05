#############################################
# lib_common-player consumer rules（FFmpeg + JNI）
#############################################
# 依据：src/main/cpp/ffmpeg_player_jni.cpp
#  1) native 方法按 "Java_com_common_player_FfmpegVideoPlayer_nativeXxx" 静态注册，
#     类的完整包名+类名与 native 方法名必须与 C++ 侧完全一致；
#  2) onNativeXxx 由 C++ 通过 GetMethodID(cls, "onNativeXxx", "签名") 查找并回调，
#     Java 侧无调用方，不保留会被 R8 直接裁剪。

# 两个播放器类名参与 JNI 符号拼接，不能改名；native 方法与 onNative* 回调
# 的方法名和签名同样不能改名。类的其他成员（构造、公开控制方法）允许优化。
-keepnames class com.common.player.FfmpegAudioPlayer
-keepnames class com.common.player.FfmpegVideoPlayer
-keepclassmembers class com.common.player.FfmpegAudioPlayer {
    native <methods>;
    private void onNative*(...);
}
-keepclassmembers class com.common.player.FfmpegVideoPlayer {
    native <methods>;
    private void onNative*(...);
}

# PlayerListener 由宿主实现并被上述两个类回调，方法名构成二进制契约。
# 依据：PlayerListener.java（onPrepared / onCompletion / onProgress / onVideoSizeChanged /
# onAudioUnavailable / onError 均由播放器主动回调）。
-keep interface com.common.player.PlayerListener { *; }

# 说明：FfmpegPlayer 仅负责 System.loadLibrary，不参与 JNI 符号映射，
# 允许 R8 裁剪、优化与改名，因此不再整包保留 com.common.player.**。
