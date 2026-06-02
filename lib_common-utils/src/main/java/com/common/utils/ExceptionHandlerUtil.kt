package com.common.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.os.Process
import android.view.Gravity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter

/**
 * 抓错误日志
 */
class ExceptionHandlerUtil private constructor() : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        handException(ex)
    }

    /**
     * 处理错误信息
     */
    private fun handException(ex: Throwable) {
        ex.printStackTrace()
        saveCrashToFile(ex)
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Process.killProcess(Process.myPid())
        }
    }

    private fun createDir() {
        val dir = File(LOG_PATH_SDCARD_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun saveCrashToFile(ex: Throwable) {
        createDir()
        val crashFile = File(LOG_PATH_SDCARD_DIR, LOG_NAME)
        try {
            if (crashFile.exists() && crashFile.length() > MAX_SIZE) {
                crashFile.delete()
                crashFile.createNewFile()
            }
            val fileOutputStream = FileOutputStream(crashFile, true)
            val printWriter = PrintWriter(fileOutputStream)
            ex.printStackTrace(printWriter)
            printWriter.append(DateFormatUtil.getShareDate())
            printWriter.append("\n\n")
            printWriter.close()
            try {
                fileOutputStream.flush()
                fileOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            fileOutputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private var LOG_PATH_SDCARD_DIR: String? = null

        private const val LOG_NAME = "crash.txt"

        private const val MAX_SIZE = 1024L * 1024 * 10

        @JvmStatic
        fun init(context: Context) {
            LOG_PATH_SDCARD_DIR = context.filesDir.path + File.separator + "crash"
            Thread.setDefaultUncaughtExceptionHandler(ExceptionHandlerUtil())
        }

        @JvmStatic
        fun doShareExceptionFile() {
            val file = File(LOG_PATH_SDCARD_DIR, LOG_NAME)
            if (!file.exists()) {
                ToastUtil.showToast(Utils.getAppContext(), "木有找到日志文件", false, Gravity.CENTER)
                return
            }
            val logUri: Uri = FileProvider.getUriForFile(
                Utils.getAppInstance(),
                Utils.getApplicationId() + ".provider", file
            )
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.putExtra("subject", "DomeProject日志")
            intent.putExtra(Intent.EXTRA_STREAM, logUri) // 添加附件，附件为file对象
            intent.type = "text/plain" // 纯文本则用text/plain的mime
            Utils.getAppContext().startActivity(intent)
        }

        @JvmStatic
        fun getExceptionFilePath(): String? = LOG_PATH_SDCARD_DIR
    }
}