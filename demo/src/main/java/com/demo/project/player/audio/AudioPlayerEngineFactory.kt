package com.demo.project.player.audio

import android.content.Context

/**
 * 音频播放引擎工厂。新增一种播放内核时，只需提供一个 [AudioPlayerEngineFactory] 实现，
 * 再通过 [AudioPlayerEngines.register] 注册即可，UI 层无需改动。
 */
fun interface AudioPlayerEngineFactory {
    fun create(context: Context): AudioPlayerEngine
}

/** 内置播放引擎类型，拓展新内核时在此追加。 */
enum class AudioEngineType {
    /** 默认：基于 Media3 ExoPlayer */
    EXO_PLAYER,

    /** 基于 lib_common-player（FFmpeg + AudioTrack） */
    FFMPEG,
}

/**
 * 音频引擎工厂注册表。
 *
 * 默认注册了 [AudioEngineType.EXO_PLAYER]，通过 [register] 可覆盖或新增实现，
 * 通过 [create] 按类型创建引擎，从而做到「默认 ExoPlayer、可拓展切换」。
 */
object AudioPlayerEngines {

    /** 全局默认引擎类型，可在 Application 启动时统一切换。 */
    @Volatile
    var defaultType: AudioEngineType = AudioEngineType.EXO_PLAYER

    private val factories = linkedMapOf(
        AudioEngineType.EXO_PLAYER to AudioPlayerEngineFactory { context ->
            ExoAudioPlayerEngine(context.applicationContext)
        },
        AudioEngineType.FFMPEG to AudioPlayerEngineFactory { context ->
            FfmpegAudioPlayerEngine(context.applicationContext)
        }
    )

    /** 注册/覆盖某类型的工厂实现。 */
    fun register(type: AudioEngineType, factory: AudioPlayerEngineFactory) {
        factories[type] = factory
    }

    /** 按类型创建引擎，未指定时使用 [defaultType]。 */
    fun create(context: Context, type: AudioEngineType = defaultType): AudioPlayerEngine {
        val factory = factories[type]
            ?: error("未注册的音频引擎类型: $type，请先调用 AudioPlayerEngines.register()")
        return factory.create(context)
    }
}
