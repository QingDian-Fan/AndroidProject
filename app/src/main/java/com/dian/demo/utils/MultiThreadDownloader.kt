package com.dian.demo.utils

import android.util.Log
import com.dian.demo.room.AppDatabase
import com.dian.demo.room.DownloadTaskEntry
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.RandomAccessFile
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
class MultiThreadDownloader(
    private val taskId: String,              // 新增任务ID
    private val url: String,
    private val saveFilePath: String,
    private val threadCount: Int = 4,
    private val callback: Callback? = null,
    private val db: AppDatabase
) {

    private val client = OkHttpClient()
    private val executor: ThreadPoolExecutor = ThreadPoolExecutor(threadCount, threadCount, 60, TimeUnit.SECONDS, LinkedBlockingQueue())
    private val downloadedBytes = AtomicLong(0)
    private var fileLength = 0L
    @Volatile private var finishedThreads = 0
    @Volatile private var isPaused = false

    interface Callback {
        fun onProgress(percent: Int)
        fun onCompleted()
        fun onError()
    }

    fun pause() { isPaused = true }
    fun resume() {
        if (!isPaused) return
        isPaused = false
        start(resume = true)
    }

    fun start(resume: Boolean = false) {
        Thread {
            val support = isSupportRange(url)
            if (!support) { callback?.onError(); return@Thread }

            fileLength = getFileLength(url)
            if (fileLength <= 0) { callback?.onError(); return@Thread }

            RandomAccessFile(saveFilePath, "rw").setLength(fileLength)

            val dao = db.downloadProgressDao()
            val progresses = if (resume) runBlocking { dao.getTaskProgress(taskId) } else emptyList()
            if (!resume) runBlocking { dao.clearTask(taskId) }

            val blockSize = fileLength / threadCount
            for (i in 0 until threadCount) {
                val start = i * blockSize
                val end = if (i == threadCount - 1) fileLength - 1 else (start + blockSize - 1)
                val downloaded = progresses.find { it.threadId == i }?.downloaded ?: 0L
                executor.execute(DownloadTask(i, start + downloaded, end))
            }
        }.start()
    }

    inner class DownloadTask(private val id: Int, private var startPos: Long, private val endPos: Long) : Runnable {
        private val blockSize = endPos - startPos + 1
        private var threadDownloaded = 0L

        override fun run() {
            val dao = db.downloadProgressDao()
            val request = Request.Builder().url(url).addHeader("Range", "bytes=$startPos-$endPos").build()
            val response = client.newCall(request).execute()
            val input = response.body?.byteStream() ?: return
            val raf = RandomAccessFile(saveFilePath, "rw")
            raf.seek(startPos)
            val buffer = ByteArray(8192)
            var len: Int

            try {
                while (input.read(buffer).also { len = it } != -1) {
                    if (isPaused) {
                        runBlocking { dao.insert(DownloadTaskEntry(taskId, id, startPos, endPos, threadDownloaded)) }
                        return
                    }

                    raf.write(buffer, 0, len)
                    threadDownloaded += len
                    downloadedBytes.addAndGet(len.toLong())
                    startPos += len

                    val threadPercent = (threadDownloaded * 100 / blockSize).toInt()
                    Log.e("Downloader", "线程 $id: $threadPercent% (${threadDownloaded}/${blockSize})")
                    val totalPercent = (downloadedBytes.get() * 100 / fileLength).toInt()
                    callback?.onProgress(totalPercent)
                }
            } finally {
                input.close()
                raf.close()
            }

            synchronized(this@MultiThreadDownloader) {
                finishedThreads++
                runBlocking { dao.insert(DownloadTaskEntry(taskId, id, startPos - threadDownloaded, endPos, threadDownloaded)) }
                if (finishedThreads == threadCount) {
                    runBlocking { dao.clearTask(taskId) }  // 下载完成后清除该任务数据
                    callback?.onCompleted()
                }
            }
        }
    }

    private fun getFileLength(url: String): Long {
        val request = Request.Builder().url(url).head().build()
        val resp = client.newCall(request).execute()
        return resp.header("Content-Length")?.toLong() ?: -1
    }

    private fun isSupportRange(url: String): Boolean {
        val request = Request.Builder().url(url).head().build()
        val resp = client.newCall(request).execute()
        val accept = resp.header("Accept-Ranges") ?: ""
        return accept.lowercase() == "bytes"
    }
}
