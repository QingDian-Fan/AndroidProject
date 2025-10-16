package com.dian.demo.utils

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogFileUtil {
    private var fos: FileOutputStream? = null
    private var writer: BufferedWriter? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())


    private val MAX_SIZE: Long = (1024 * 1024 * 10).toLong()

    private val lock = Any()

    fun init(context: Context) {
        synchronized(lock) {
            try {
                fos?.close()
                val logDir = File(context.filesDir, "log")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                val logFile = File(logDir, "log.txt")
                if (logFile.exists() && logFile.length() > MAX_SIZE) {
                    logFile.delete()
                    logFile.createNewFile()
                }
                writer = BufferedWriter(FileWriter(logFile, true))
                fos = FileOutputStream(logFile, true)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun writeMessage(message: String) {
        val output = writer ?: return
        val logLine = buildString {
            append(dateFormat.format(Date()))
            append("    ")
            append(message)
            append("\r\n")
        }

        synchronized(lock) {
            try {
                output.write(logLine)
                output.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun close() {
        synchronized(lock) {
            try {
                writer?.close()
                writer = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}