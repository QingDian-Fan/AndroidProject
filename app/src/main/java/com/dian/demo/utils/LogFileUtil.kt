package com.dian.demo.utils

import android.content.Context
import android.content.Intent
import android.view.Gravity
import androidx.core.content.FileProvider
import com.dian.demo.ProjectApplication.Companion.getAppContext
import com.dian.demo.ProjectApplication.Companion.getAppInstance
import com.dian.demo.utils.ToastUtil.showToast
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

    private var filePath: String? = null


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
                filePath = logFile.absolutePath
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

    fun getLogFilePath() = filePath

    fun doShareLogFile() {
        val file = File(filePath, "log.txt")
        if (!file.exists()) {
            showToast(getAppContext(), "木有找到日志文件", false, Gravity.CENTER)
            return
        }
        //Uri logUri = Uri.parse(file.getAbsolutePath());
        val logUri = FileProvider.getUriForFile(
            getAppInstance(),
            getAppContext().packageName + ".provider", file
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("subject", "DomeProject日志")
        intent.putExtra(Intent.EXTRA_STREAM, logUri)
        intent.setType("text/plain")
        getAppContext().startActivity(intent)
    }

}