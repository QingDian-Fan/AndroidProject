package com.common.utils.audio

import android.media.AudioManager
import android.media.AudioTrack
import com.common.utils.LogUtil
import java.io.IOException
import java.io.RandomAccessFile

class AudioTrackManager {

    private var audioTrack: AudioTrack? = null

    @Volatile
    private var playing = false

    fun playWav(filepath: String, callback: RecordCallback?): AudioParams? {
        val file: RandomAccessFile = try {
            RandomAccessFile(filepath, "r")
        } catch (e: IOException) {
            LogUtil.e(TAG, "打开播放文件失败: $filepath, ${e.message}")
            return null
        }
        val params: AudioParams = try {
            readWavHeader(file)
        } catch (e: IOException) {
            LogUtil.e(TAG, "解析 WAV 文件头失败: ${e.message}")
            try {
                file.close()
            } catch (ignored: IOException) {
            }
            return null
        }
        val simpleRate = params.simpleRate
        val channelConfig = params.outChannelConfig
        val audioFormat = params.encodingFormat
        val minBufSize = AudioTrack.getMinBufferSize(simpleRate, channelConfig, audioFormat)
        val track = AudioTrack(
            AudioManager.STREAM_MUSIC, simpleRate, channelConfig, audioFormat, minBufSize,
            AudioTrack.MODE_STREAM
        )
        audioTrack = track
        Thread {
            playing = true
            track.play()
            val buffer = ByteArray(minBufSize)
            try {
                file.seek(44)
                while (playing) {
                    val read = file.read(buffer)
                    if (read > 0) {
                        track.write(buffer, 0, read)
                    } else {
                        file.close()
                        playing = false
                        track.stop()
                        track.release()
                    }
                    callback?.onRecord(buffer, read)
                }
            } catch (e: IOException) {
                LogUtil.e(TAG, "播放过程中读取文件失败: ${e.message}")
            }
        }.start()
        return params
    }

    fun stop() {
        playing = false
    }

    companion object {
        private const val TAG = "AudioTrackManager"

        @Throws(IOException::class)
        fun readWavHeader(file: RandomAccessFile): AudioParams {
            file.seek(22)
            val channelCount = file.readByte()
            file.seek(24)
            var sampleRate = file.readByte().toInt() and 0xff
            sampleRate = sampleRate or ((file.readByte().toInt() and 0xff) shl 8)
            sampleRate = sampleRate or ((file.readByte().toInt() and 0xff) shl 16)
            sampleRate = sampleRate or ((file.readByte().toInt() and 0xff) shl 24)

            file.seek(34)
            val bits = file.readByte()
            return AudioParams(sampleRate, channelCount.toInt(), bits.toInt())
        }
    }
}