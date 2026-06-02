package com.common.utils.audio

import android.media.AudioFormat

class AudioParams {

    enum class Format {
        SINGLE_8_BIT, DOUBLE_8_BIT, SINGLE_16_BIT, DOUBLE_16_BIT
    }

    private var format: Format

    var simpleRate: Int

    constructor(simpleRate: Int, f: Format) {
        this.simpleRate = simpleRate
        this.format = f
    }

    constructor(simpleRate: Int, channelCount: Int, bits: Int) {
        this.simpleRate = simpleRate
        this.format = toFormat(channelCount, bits)
    }

    val bits: Int
        get() = if (format == Format.SINGLE_8_BIT || format == Format.DOUBLE_8_BIT) 8 else 16

    val encodingFormat: Int
        get() = if (format == Format.SINGLE_8_BIT || format == Format.DOUBLE_8_BIT) {
            AudioFormat.ENCODING_PCM_8BIT
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }

    val channelCount: Int
        get() = if (format == Format.SINGLE_8_BIT || format == Format.SINGLE_16_BIT) 1 else 2

    val channelConfig: Int
        get() = if (format == Format.SINGLE_8_BIT || format == Format.SINGLE_16_BIT) {
            AudioFormat.CHANNEL_IN_MONO
        } else {
            AudioFormat.CHANNEL_IN_STEREO
        }

    val outChannelConfig: Int
        get() = if (format == Format.SINGLE_8_BIT || format == Format.SINGLE_16_BIT) {
            AudioFormat.CHANNEL_OUT_MONO
        } else {
            AudioFormat.CHANNEL_OUT_STEREO
        }

    private fun toFormat(channelCount: Int, bits: Int): Format {
        require((channelCount == 1 || channelCount == 2) && (bits == 8 || bits == 16)) {
            "不支持其它格式 channelCount=$channelCount bits=$bits"
        }
        return if (channelCount == 1) {
            if (bits == 8) Format.SINGLE_8_BIT else Format.SINGLE_16_BIT
        } else {
            if (bits == 8) Format.DOUBLE_8_BIT else Format.DOUBLE_16_BIT
        }
    }
}