package com.common.utils.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.common.utils.LogUtil
import com.common.utils.Utils
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class AudioRecordManager {

    private val defaultFormat = AudioParams(16000, 1, 16)

    private var record: AudioRecord? = null

    private var recordThread: Thread? = null

    @Volatile
    private var recording = false

    private var file: RandomAccessFile? = null

    fun startRecord(filePath: String?, callback: RecordCallback?) {
        startRecord(filePath, defaultFormat, callback)
    }

    private fun startRecord(filePath: String?, params: AudioParams, callback: RecordCallback?) {
        val channelCount = params.channelCount
        val bits = params.bits

        val storeFile = !filePath.isNullOrEmpty()

        startRecord(params) { bytes, len ->
            if (storeFile) {
                if (file == null) {
                    val f = File(filePath!!)
                    if (f.exists() && !f.delete()) {
                        LogUtil.e(TAG, "删除已存在录音文件失败: $filePath")
                    }
                    try {
                        file = RandomAccessFile(f, "rw").apply {
                            write(getWaveFileHeader(0, params.simpleRate, channelCount, bits))
                        }
                    } catch (e: IOException) {
                        LogUtil.e(TAG, "创建录音文件并写入文件头失败: ${e.message}")
                    }
                }
                if (len > 0) {
                    try {
                        file?.write(bytes, 0, len)
                    } catch (e: IOException) {
                        LogUtil.e(TAG, "写入录音数据失败: ${e.message}")
                    }
                } else {
                    try {
                        file?.let {
                            // 因为在前面已经写入头信息，所以这里要减去头信息才是数据的长度
                            val length = it.length().toInt() - 44
                            it.seek(0)
                            it.write(getWaveFileHeader(length, params.simpleRate, channelCount, bits))
                            it.close()
                        }
                        file = null
                    } catch (e: IOException) {
                        LogUtil.e(TAG, "回填录音文件头/关闭文件失败: ${e.message}")
                    }
                }
            }
            callback?.onRecord(bytes, len)
        }
    }

    private fun startRecord(params: AudioParams, callback: RecordCallback?) {
        val simpleRate = params.simpleRate
        val channelConfig = params.channelConfig
        val audioFormat = params.encodingFormat
        // 根据 AudioRecord 提供的 api 拿到最小缓存大小
        val bufferSize = AudioRecord.getMinBufferSize(simpleRate, channelConfig, audioFormat)
        if (ActivityCompat.checkSelfPermission(
                Utils.getAppContext(),
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(Utils.getAppContext(), "请赋予RECORD_AUDIO权限", Toast.LENGTH_LONG).show()
            return
        }
        // 创建 Record 对象
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, simpleRate, channelConfig, audioFormat, bufferSize
        )
        record = audioRecord
        recordThread = Thread {
            val buffer = ByteArray(bufferSize)
            audioRecord.startRecording()
            recording = true
            while (recording) {
                val read = audioRecord.read(buffer, 0, bufferSize)
                // 将数据回调到外部
                if (read > 0) {
                    callback?.onRecord(buffer, read)
                }
            }
            // len 为 -1 时表示结束
            callback?.onRecord(buffer, -1)
            // 释放资源
            release()
        }.also { it.start() }
    }

    fun stop() {
        recording = false
    }

    fun release() {
        recording = false
        record?.let {
            it.stop()
            it.release()
        }
        record = null
        file = null
        recordThread = null
    }

    companion object {
        private const val TAG = "AudioRecordManager"

        private fun getWaveFileHeader(
            totalDataLen: Int,
            sampleRate: Int,
            channelCount: Int,
            bits: Int
        ): ByteArray {
            val header = ByteArray(44)
            // RIFF/WAVE header
            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()

            val fileLength = totalDataLen + 36
            header[4] = (fileLength and 0xff).toByte()
            header[5] = (fileLength shr 8 and 0xff).toByte()
            header[6] = (fileLength shr 16 and 0xff).toByte()
            header[7] = (fileLength shr 24 and 0xff).toByte()
            // WAVE
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            // 'fmt ' chunk
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            // 4 bytes: size of 'fmt ' chunk
            header[16] = 16
            header[17] = 0
            header[18] = 0
            header[19] = 0

            // pcm format = 1
            header[20] = 1
            header[21] = 0
            header[22] = channelCount.toByte()
            header[23] = 0

            header[24] = (sampleRate and 0xff).toByte()
            header[25] = (sampleRate shr 8 and 0xff).toByte()
            header[26] = (sampleRate shr 16 and 0xff).toByte()
            header[27] = (sampleRate shr 24 and 0xff).toByte()

            val byteRate = sampleRate * bits * channelCount / 8
            header[28] = (byteRate and 0xff).toByte()
            header[29] = (byteRate shr 8 and 0xff).toByte()
            header[30] = (byteRate shr 16 and 0xff).toByte()
            header[31] = (byteRate shr 24 and 0xff).toByte()
            // block align
            header[32] = (channelCount * bits / 8).toByte()
            header[33] = 0
            // bits per sample
            header[34] = bits.toByte()
            header[35] = 0
            // data
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (totalDataLen and 0xff).toByte()
            header[41] = (totalDataLen shr 8 and 0xff).toByte()
            header[42] = (totalDataLen shr 16 and 0xff).toByte()
            header[43] = (totalDataLen shr 24 and 0xff).toByte()
            return header
        }
    }
}