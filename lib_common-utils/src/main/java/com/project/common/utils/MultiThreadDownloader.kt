package com.project.common.utils


import android.util.Log
import com.dian.demo.room.DownloadTaskDao
import com.dian.demo.room.DownloadTaskEntry
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.sumOf

/**
 * 下载器：
 * - 调用 start() 不需要传参数，自动从 DB 恢复
 * - 找到旧任务 → 自动继续下载
 * - 找不到 → 新建任务从头下载
 */
class MultiThreadDownloader(
    private val taskId: String,               // 任务唯一ID
    private val url: String,
    private val savePath: String,
    private val threadCount: Int = 4,
    private val db: DownloadTaskDao,             // Room Dao
    private val callback: Callback? = null
) {

    interface Callback {
        fun onProgress(percent: Int)
        fun onCompleted()
        fun onError(msg: String)
    }

    private val client = OkHttpClient()
    private val downloadedBytes = AtomicLong(0)
    private var totalFileLength = 0L

    @Volatile
    private var pauseFlag = false

    @Volatile
    private var finishedThreads = 0

    private val executor: ThreadPoolExecutor = ThreadPoolExecutor(
        threadCount, threadCount,
        60, TimeUnit.SECONDS,
        LinkedBlockingQueue()
    )

    /** =================== 入口：start ==================== */
    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            pauseFlag = false

            val existsProgress = db.getTask(taskId)

            if (!existsProgress.isNullOrEmpty() && File(savePath).exists()) {
                Log.e("DL", "发现历史任务 → 自动续传")

                resumeFromDB(existsProgress)
            } else {
                Log.e("DL", "无历史任务 → 重新开始下载")

                startNewDownload()
            }
        }
    }

    /** =================== 暂停 ==================== */
    fun pause() {
        pauseFlag = true
        Log.e("DL", "暂停请求已发出")
    }

    /** ==========================================================
     *                  1) 新建下载任务
     * ========================================================== */
    private suspend fun startNewDownload() {
        val fileLength = getFileLength(url)

        if (fileLength <= 0) {
            callback?.onError("无法获取文件大小")
            return
        }
        totalFileLength = fileLength

        // 创建空文件
        RandomAccessFile(savePath, "rw").setLength(fileLength)

        // 创建线程任务记录
        val blockSize = fileLength / threadCount

        val progressList = mutableListOf<DownloadTaskEntry>()

        for (i in 0 until threadCount) {
            val start = i * blockSize
            val end = if (i == threadCount - 1) fileLength - 1 else (start + blockSize - 1)

            progressList.add(
                DownloadTaskEntry(
                    taskId = taskId,
                    threadId = i,
                    startPos = start,
                    endPos = end,
                    downloaded = 0
                )
            )
        }

        db.insertAll(progressList)

        Log.e("DL", "数据库初始化完毕，启动线程池")

        launchWorkers(progressList)
    }

    /** ==========================================================
     *                  2) 恢复下载任务
     * ========================================================== */
    private suspend fun resumeFromDB(list: List<DownloadTaskEntry>) {
        totalFileLength = list.maxOf { it.endPos + 1 }

        downloadedBytes.set(list.sumOf { it.downloaded })

        launchWorkers(list)
    }


    /** ==========================================================
     *                  3) 启动线程池进行下载
     * ========================================================== */
    private suspend fun launchWorkers(list: List<DownloadTaskEntry>) {
        finishedThreads = 0

        list.forEach { progress ->
            executor.execute(DownloadTask(progress))
        }
    }

    /** ==========================================================
     *                  下载线程任务
     * ========================================================== */
    inner class DownloadTask(private val info: DownloadTaskEntry) : Runnable {

        override fun run() {
            try {

                val realStart = info.startPos + info.downloaded
                if (realStart > info.endPos) {
                    markThreadFinish()
                    return
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Range", "bytes=$realStart-${info.endPos}")
                    .build()

                val response = client.newCall(request).execute()
                val input = response.body?.byteStream() ?: return

                val raf = RandomAccessFile(savePath, "rw")
                raf.seek(realStart)

                val buffer = ByteArray(8192)
                var len: Int = 0

                while (!pauseFlag && input.read(buffer).also { len = it } != -1) {
                    raf.write(buffer, 0, len)

                    // 持久化该线程的偏移量
                    info.downloaded += len
                    CoroutineScope(Dispatchers.IO).launch {
                        db.update(info)
                    }
                    val total = downloadedBytes.addAndGet(len.toLong())
                    val percent = (total * 100 / totalFileLength).toInt()

                    callback?.onProgress(percent)
                }

                input.close()
                raf.close()

                if (pauseFlag) {
                    Log.e("DL", "线程 ${info.threadId} 暂停")
                    return
                }

                markThreadFinish()

            } catch (e: Exception) {
                e.printStackTrace()
                callback?.onError(e.message ?: "下载失败")
            }
        }
    }

    /** ==========================================================
     *            全部线程完成 → 下载完成 → 清理数据库
     * ========================================================== */
    @Synchronized
    private fun markThreadFinish() {
        finishedThreads++
        if (finishedThreads == threadCount) {
            Log.e("DL", "全部线程下载完毕 → 任务完成")
            runBlocking {
                db.deleteTask(taskId)
            }
            callback?.onCompleted()
        }
    }

    /** ========================================================== */
    private fun getFileLength(url: String): Long {
        val req = Request.Builder().url(url).head().build()
        val resp = client.newCall(req).execute()
        return resp.header("Content-Length")?.toLong() ?: -1
    }
}
