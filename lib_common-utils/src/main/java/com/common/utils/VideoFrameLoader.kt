package com.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger



/**
 * 视频帧加载器
 *
 * 特性：
 * 1. 渐进式加载：单个 MediaMetadataRetriever 串行抽取预览帧和高清帧，预览先显示，高清后替换
 * 2. 二级缓存：内存缓存(LruCache) + 磁盘缓存(File)
 * 3. 回调区分：onResult 回调中可区分是预览图还是高清图
 * 4. OOM 防护：高清图限制最大尺寸，避免 4K 视频撑爆内存
 *
 * v3.0 优化点：
 * - 每个视频只建立一次网络连接（原先预览+高清各一次）
 * - 线程占用从 2个/视频 降到 1个/视频
 * - 消除预览/高清并行竞态条件
 * - 自定义拒绝策略：队列满时丢弃最旧任务并记录日志，避免阻塞主线程
 * - 高清图 API 27+ 直接按目标尺寸解码，避免分配原始大 Bitmap
 * - 高清图增加尺寸上限，防止 OOM
 * - 修复 executor shutdown 不彻底的问题
 */
object VideoFrameLoader {

    private const val TAG = "VideoFrameLoader"

    /** 是否开启日志（生产环境建议设为 false） */
    var DEBUG = false

    // ================= 配置常量 =================
    /** 预览图尺寸 */
    private const val PREVIEW_SIZE = 160

    /** 高清图最大尺寸（限制到 720p~1080p 之间，防止 OOM） */
    private const val HD_MAX_SIZE = 1280

    private const val MEMORY_CACHE_BYTES = 48 * 1024 * 1024
    private const val DISK_CACHE_MAX_BYTES = 50L * 1024 * 1024
    private const val DISK_CACHE_DIR = "video_thumbs"
    private const val CACHE_MAX_AGE_DAYS = 7
    /** 抽帧失败退避窗口，避免同一视频反复重试 */
    private const val FAILURE_BACKOFF_MS = 10 * 60 * 1000L
    /** Glide 兜底最大尺寸，控制解码内存占用 */
    private const val GLIDE_FALLBACK_SIZE = 720
    /** Glide 网络超时，避免兜底链路长时间占线程 */
    private const val GLIDE_FALLBACK_TIMEOUT_MS = 6000
    /** Glide 同步等待上限 */
    private const val GLIDE_FALLBACK_WAIT_SECONDS = 8L
    /** 仅对这些后缀尝试 OSS 视频截帧 */
    private val VIDEO_SUFFIXES = arrayOf(".mp4", ".mov", ".m4v", ".3gp", ".webm")

    // ================= 线程池 =================
    /**
     * 自定义拒绝策略：队列满时丢弃最旧的等待任务，将新任务入队。
     * 
     * 不能用 CallerRunsPolicy —— load() 在主线程调用，会导致 setDataSource/getFrameAtTime
     * 等重操作在主线程执行，引发 ANR。
     * 不能用 DiscardPolicy —— 静默丢弃当前任务，用户看不到封面且无反馈。
     * 此策略优先保证最新提交的任务（通常是当前可见的 item）能被执行。
     */
    private val rejectedHandler = RejectedExecutionHandler { r, executor ->
        if (!executor.isShutdown) {
            // 丢弃队列头部最旧的任务（通常是已滚出屏幕的 item）
            val discarded = executor.queue.poll()
            if (discarded != null) {
                log("队列满，丢弃最旧任务，将新任务入队")
            }
            // 尝试将新任务重新入队
            if (!executor.queue.offer(r)) {
                log("入队仍然失败，丢弃新任务")
            }
        }
    }

    private val executor: ThreadPoolExecutor by lazy {
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        ThreadPoolExecutor(
            cores.coerceAtMost(4),
            (cores * 2).coerceAtMost(8),
            30L, TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>(128),
            ThreadFactory { r -> Thread(r, "VideoFrameLoader").apply { priority = Thread.NORM_PRIORITY } },
            rejectedHandler
        ).apply { allowCoreThreadTimeOut(true) }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // ================= 缓存 =================
    private val memoryCache = object : LruCache<String, Bitmap>(MEMORY_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    @Volatile
    private var diskCacheDir: File? = null

    private fun getDiskCacheDir(context: Context): File {
        return diskCacheDir ?: synchronized(this) {
            diskCacheDir ?: File(context.cacheDir, DISK_CACHE_DIR).also {
                if (!it.exists()) it.mkdirs()
                diskCacheDir = it
            }
        }
    }

    // ================= 任务管理 =================
    private data class LoadTask(
        val id: Int,
        val url: String,
        val hdLoaded: AtomicBoolean = AtomicBoolean(false),
        var future: Future<*>? = null
    )

    private val taskIdGenerator = AtomicInteger(0)
    // 使用 ConcurrentHashMap 替代 Collections.synchronizedMap(WeakHashMap())
    // 避免复合操作（check-then-act）的并发安全问题
    private val tasks = ConcurrentHashMap<Int, LoadTask>()
    // ImageView hashCode -> taskId 的映射，用于取消和校验
    private val viewTaskMap = ConcurrentHashMap<Int, Int>()
    // key(hdKey) -> fail timestamp
    private val failureBackoffMap = ConcurrentHashMap<String, Long>()

    private fun log(msg: String) {
        if (DEBUG) Log.d(TAG, msg)
    }

    private fun viewKey(iv: ImageView): Int = System.identityHashCode(iv)

    // ================= 对外 API =================

    /**
     * 加载视频缩略图
     *
     * @param url 视频URL
     * @param imageView 目标ImageView
     * @param frameUs 抽帧时间点（微秒）
     * @param headers HTTP请求头
     * @param onResult 结果回调，参数：(ImageView, Bitmap?, isHd: Boolean)
     *                 isHd=false 表示预览图，isHd=true 表示高清图
     */
    fun load(
        url: String,
        imageView: ImageView,
        frameUs: Long = 500_000L,
        headers: Map<String, String>? = null,
        onResult: ((iv: ImageView, bmp: Bitmap?, isHd: Boolean) -> Unit)? = null
    ) {
        val requestUrl = url.trim()
        val sourceUrl = normalizeVideoUrl(requestUrl)
        val taskId = taskIdGenerator.incrementAndGet()
        log("[$taskId] load() 开始, url=${sourceUrl.takeLast(40)}")

        cancel(imageView)
        // tag 保持原始请求 URL，避免外部已有比较逻辑失效
        imageView.setTag(R.id.tag_video_url, requestUrl)

        val appCtx = imageView.context.applicationContext
        val ivRef = WeakReference(imageView)
        val ivKey = viewKey(imageView)

        val hdKey = cacheKey(sourceUrl, frameUs, "hd")

        // 1. 检查内存缓存
        memoryCache.get(hdKey)?.let { cached ->
            log("[$taskId] 内存缓存命中")
            if (requestUrl == (imageView.getTag(R.id.tag_video_url) as? String)) {
                clearFailureMark(hdKey)
                deliverResult(imageView, cached, true, onResult)
            }
            return
        }

        // 2. 检查磁盘缓存
        val diskFile = File(getDiskCacheDir(appCtx), hdKey.md5() + ".jpg")
        if (diskFile.exists()) {
            log("[$taskId] 磁盘缓存命中")
            val task = LoadTask(taskId, requestUrl)
            tasks[taskId] = task
            viewTaskMap[ivKey] = taskId

            task.future = executor.submit {
                try {
                    val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                    if (bitmap != null) {
                        memoryCache.put(hdKey, bitmap)
                        clearFailureMark(hdKey)
                        mainHandler.post {
                            val iv = ivRef.get()
                            if (iv == null) {
                                cleanupTaskById(taskId, ivKey)
                                return@post
                            }
                            if (isTaskValid(iv, task)) {
                                log("[$taskId] 磁盘缓存加载成功, 尺寸=${bitmap.width}x${bitmap.height}")
                                task.hdLoaded.set(true)
                                deliverResult(iv, bitmap, true, onResult)
                                cleanupTask(iv, task)
                            }
                        }
                    } else {
                        log("[$taskId] 磁盘缓存损坏，删除")
                        diskFile.delete()
                        // 磁盘缓存损坏，fallback 到网络抽帧
                        startProgressiveLoad(
                            taskId = taskId,
                            requestUrl = requestUrl,
                            sourceUrl = sourceUrl,
                            frameUs = frameUs,
                            headers = headers,
                            appCtx = appCtx,
                            ivRef = ivRef,
                            ivKey = ivKey,
                            hdKey = hdKey,
                            diskFile = diskFile,
                            onResult = onResult
                        )
                    }
                } catch (e: Throwable) {
                    log("[$taskId] 磁盘缓存读取失败: ${e.message}")
                    diskFile.delete()
                    // 磁盘读失败也尝试回落到抽帧链路
                    startProgressiveLoad(
                        taskId = taskId,
                        requestUrl = requestUrl,
                        sourceUrl = sourceUrl,
                        frameUs = frameUs,
                        headers = headers,
                        appCtx = appCtx,
                        ivRef = ivRef,
                        ivKey = ivKey,
                        hdKey = hdKey,
                        diskFile = diskFile,
                        onResult = onResult
                    )
                }
            }
            return
        }

        if (isInFailureBackoff(hdKey)) {
            log("[$taskId] 命中失败退避窗口，跳过抽帧")
            if (requestUrl == (imageView.getTag(R.id.tag_video_url) as? String)) {
                deliverResult(imageView, null, true, onResult)
            }
            return
        }

        // 3. 无缓存，渐进式加载
        log("[$taskId] 无缓存，启动渐进式加载")
        startProgressiveLoad(
            taskId = taskId,
            requestUrl = requestUrl,
            sourceUrl = sourceUrl,
            frameUs = frameUs,
            headers = headers,
            appCtx = appCtx,
            ivRef = ivRef,
            ivKey = ivKey,
            hdKey = hdKey,
            diskFile = diskFile,
            onResult = onResult
        )
    }

    /**
     * 兼容旧版 API（不带 isHd 参数的回调）
     */
    @JvmOverloads
    fun load(
        url: String,
        imageView: ImageView,
        frameUs: Long = 500_000L,
        headers: Map<String, String>? = null,
        onResult: ((iv: ImageView, bmp: Bitmap?) -> Unit)?
    ) {
        load(url, imageView, frameUs, headers) { iv, bmp, _ ->
            onResult?.invoke(iv, bmp)
        }
    }

    /**
     * 渐进式加载：单个 MediaMetadataRetriever 串行抽取预览帧和高清帧
     * 
     * 相比旧版（两个 retriever 并行），优势：
     * - 只建立一次网络连接（setDataSource 是最耗时的部分）
     * - 只占用一个线程
     * - 无竞态条件
     */
    private fun startProgressiveLoad(
        taskId: Int,
        requestUrl: String,
        sourceUrl: String,
        frameUs: Long,
        headers: Map<String, String>?,
        appCtx: Context,
        ivRef: WeakReference<ImageView>,
        ivKey: Int,
        hdKey: String,
        diskFile: File,
        onResult: ((iv: ImageView, bmp: Bitmap?, isHd: Boolean) -> Unit)?
    ) {
        if (ivRef.get() == null) return
        val task = LoadTask(taskId, requestUrl)
        tasks[taskId] = task
        viewTaskMap[ivKey] = taskId

        val previewKey = cacheKey(sourceUrl, frameUs, "preview")

        // === 单任务：串行 预览-----> 高清 ===
        task.future = executor.submit {
            var retriever: MediaMetadataRetriever? = null
            val startTime = System.currentTimeMillis()
            var preview: Bitmap? = null
            log("[$taskId] 渐进式加载任务开始")

            try {
                if (Thread.interrupted()) {
                    log("[$taskId] 任务被取消")
                    return@submit
                }

                // ====== 建立连接（整个任务只需这一次） ======
                retriever = MediaMetadataRetriever()
                setDataSourceSmart(retriever, appCtx, sourceUrl, headers)
                log("[$taskId] setDataSource 耗时=${System.currentTimeMillis() - startTime}ms")

                if (Thread.interrupted()) return@submit

                // ====== 阶段1：快速抽取预览图 ======
                preview = memoryCache.get(previewKey)
                if (preview != null) {
                    log("[$taskId] 预览图内存缓存命中")
                } else {
                    val previewStart = System.currentTimeMillis()
                    preview = extractFrameFromRetriever(retriever, frameUs, PREVIEW_SIZE)
                    if (preview != null) {
                        memoryCache.put(previewKey, preview)
                        log("[$taskId] 预览图抽帧成功, 耗时=${System.currentTimeMillis() - previewStart}ms, 尺寸=${preview.width}x${preview.height}")
                    } else {
                        log("[$taskId] 预览图抽帧失败")
                    }
                }

                // 先把预览图推到主线程显示
                if (preview != null && !Thread.interrupted()) {
                    val previewBmp = preview
                    mainHandler.post {
                        val view = ivRef.get()
                        if (view == null) {
                            // ImageView 已被 GC，但不清理任务——高清图阶段仍在进行
                            return@post
                        }
                        if (isTaskValid(view, task) && !task.hdLoaded.get()) {
                            log("[$taskId] 显示预览图")
                            deliverResult(view, previewBmp, false, onResult)
                        }
                    }
                }

                if (Thread.interrupted()) return@submit

                // ====== 阶段2：抽取高清图（复用同一个 retriever，无需重新建连） ======
                val hdStart = System.currentTimeMillis()
                // API 27+ 直接按目标尺寸解码，避免分配原始大 Bitmap 导致低端机 OOM
                @Suppress("KotlinConstantConditions")
                val hdBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && HD_MAX_SIZE > 0) {
                    retriever.getScaledFrameAtTime(
                        frameUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        HD_MAX_SIZE, HD_MAX_SIZE
                    )
                } else {
                    // API 26 以下无法按目标尺寸解码，先取原图再缩放
                    retriever.getFrameAtTime(
                        frameUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )?.let { limitBitmapSize(it, HD_MAX_SIZE) }
                }

                if (Thread.interrupted()) return@submit

                if (hdBitmap != null) {
                    log("[$taskId] 高清图抽帧成功, 耗时=${System.currentTimeMillis() - hdStart}ms, 尺寸=${hdBitmap.width}x${hdBitmap.height}")
                    memoryCache.put(hdKey, hdBitmap)
                    clearFailureMark(hdKey)
                    saveToDiskAsync(hdBitmap, diskFile)

                    mainHandler.post {
                        val view = ivRef.get()
                        if (view == null) {
                            cleanupTaskById(taskId, ivKey)
                            return@post
                        }
                        if (isTaskValid(view, task)) {
                            task.hdLoaded.set(true)
                            log("[$taskId] 显示高清图")
                            deliverResult(view, hdBitmap, true, onResult)
                            cleanupTask(view, task)
                        }
                    }
                } else {
                    log("[$taskId] 高清图抽帧失败")
                    val previewBmp = preview
                    if (previewBmp != null) {
                        // HD 失败时将预览图提升为终态，避免每次 bind 都重复抽帧
                        memoryCache.put(hdKey, previewBmp)
                        clearFailureMark(hdKey)
                        saveToDiskAsync(previewBmp, diskFile)

                        mainHandler.post {
                            val view = ivRef.get()
                            if (view == null) {
                                cleanupTaskById(taskId, ivKey)
                                return@post
                            }
                            if (isTaskValid(view, task)) {
                                task.hdLoaded.set(true)
                                log("[$taskId] 使用预览图作为终态兜底")
                                deliverResult(view, previewBmp, true, onResult)
                                cleanupTask(view, task)
                            }
                        }
                    } else {
                        val glideBmp = extractFrameByGlide(appCtx, sourceUrl, frameUs, GLIDE_FALLBACK_SIZE)
                        if (glideBmp != null) {
                            log("[$taskId] Glide 兜底抽帧成功, 尺寸=${glideBmp.width}x${glideBmp.height}")
                            memoryCache.put(hdKey, glideBmp)
                            clearFailureMark(hdKey)
                            saveToDiskAsync(glideBmp, diskFile)

                            mainHandler.post {
                                val view = ivRef.get()
                                if (view == null) {
                                    cleanupTaskById(taskId, ivKey)
                                    return@post
                                }
                                if (isTaskValid(view, task)) {
                                    task.hdLoaded.set(true)
                                    deliverResult(view, glideBmp, true, onResult)
                                    cleanupTask(view, task)
                                }
                            }
                        } else {
                            markFailure(hdKey)
                            mainHandler.post {
                                val view = ivRef.get()
                                if (view == null) {
                                    cleanupTaskById(taskId, ivKey)
                                    return@post
                                }
                                if (isTaskValid(view, task)) {
                                    cleanupTask(view, task)
                                    log("[$taskId] 预览/高清/Glide 兜底均失败")
                                    deliverResult(view, null, true, onResult)
                                }
                            }
                        }
                    }
                }

                log("[$taskId] 渐进式加载任务完成, 总耗时=${System.currentTimeMillis() - startTime}ms")
            } catch (e: Throwable) {
                log("[$taskId] 渐进式加载任务异常: ${e.message}")
                if (Thread.currentThread().isInterrupted) return@submit

                val fallback = preview ?: extractFrameByGlide(appCtx, sourceUrl, frameUs, GLIDE_FALLBACK_SIZE)
                if (fallback != null) {
                    memoryCache.put(hdKey, fallback)
                    clearFailureMark(hdKey)
                    saveToDiskAsync(fallback, diskFile)
                    mainHandler.post {
                        val view = ivRef.get()
                        if (view == null) {
                            cleanupTaskById(taskId, ivKey)
                            return@post
                        }
                        if (isTaskValid(view, task)) {
                            task.hdLoaded.set(true)
                            log("[$taskId] 异常后兜底成功")
                            deliverResult(view, fallback, true, onResult)
                            cleanupTask(view, task)
                        }
                    }
                } else {
                    markFailure(hdKey)
                    mainHandler.post {
                        val view = ivRef.get()
                        if (view == null) {
                            cleanupTaskById(taskId, ivKey)
                            return@post
                        }
                        if (isTaskValid(view, task)) {
                            cleanupTask(view, task)
                            deliverResult(view, null, true, onResult)
                        }
                    }
                }
            } finally {
                releaseRetriever(retriever)
            }
        }
    }

    private fun isTaskValid(iv: ImageView, task: LoadTask): Boolean {
        val currentTaskId = viewTaskMap[viewKey(iv)]
        val currentUrl = iv.getTag(R.id.tag_video_url) as? String
        return currentTaskId == task.id && currentUrl == task.url
    }

    private fun cleanupTask(iv: ImageView, task: LoadTask) {
        tasks.remove(task.id)
        viewTaskMap.remove(viewKey(iv), task.id)
    }

    /** 当 ImageView 已被 GC 时，通过 taskId 和 ivKey 清理残留 */
    private fun cleanupTaskById(taskId: Int, ivKey: Int) {
        tasks.remove(taskId)
        viewTaskMap.remove(ivKey, taskId)
    }

    /**
     * 从已初始化的 retriever 中抽取指定尺寸的帧
     */
    private fun extractFrameFromRetriever(
        retriever: MediaMetadataRetriever,
        frameUs: Long,
        size: Int
    ): Bitmap? {
        return try {
            if (Thread.interrupted()) return null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                retriever.getScaledFrameAtTime(
                    frameUs,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    size, size
                )
            } else {
                retriever.getFrameAtTime(frameUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?.let { src ->
                        val scaled = scaleDown(src, size)
                        if (scaled !== src) src.recycle()
                        scaled
                    }
            }
        } catch (e: Throwable) {
            log("extractFrameFromRetriever 异常: ${e.message}")
            null
        }
    }

    /**
     * 安全释放 MediaMetadataRetriever，兼容不同 API 版本
     */
    private fun releaseRetriever(retriever: MediaMetadataRetriever?) {
        if (retriever == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                retriever.close()
            } else {
                @Suppress("DEPRECATION")
                retriever.release()
            }
        } catch (_: Throwable) {
        }
    }

    private fun saveToDiskAsync(bitmap: Bitmap, file: File) {
        executor.submit {
            try {
                val tempFile = File(file.parent, "${file.name}.tmp")
                FileOutputStream(tempFile).use { fos ->
                    // 高分辨率图使用 90% 质量
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                    fos.flush()
                }
                val renamed = tempFile.renameTo(file)
                if (!renamed) {
                    // renameTo 可能在某些文件系统上失败，fallback 到 copy
                    log("renameTo 失败，尝试 copy")
                    tempFile.inputStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile.delete()
                }
                log("磁盘缓存保存成功: ${file.name}, 大小=${file.length() / 1024}KB")
                cleanupDiskCacheIfNeeded(file.parentFile)
            } catch (e: Throwable) {
                log("磁盘缓存保存失败: ${e.message}")
            }
        }
    }

    private fun cleanupDiskCacheIfNeeded(cacheDir: File?) {
        cacheDir ?: return
        try {
            val files = cacheDir.listFiles { f -> f.isFile && f.name.endsWith(".jpg") } ?: return
            val now = System.currentTimeMillis()
            val maxAgeMs = CACHE_MAX_AGE_DAYS * 24 * 60 * 60 * 1000L

            var totalSize = 0L
            val validFiles = files.filter { f ->
                if (now - f.lastModified() > maxAgeMs) {
                    f.delete()
                    false
                } else {
                    totalSize += f.length()
                    true
                }
            }.sortedBy { it.lastModified() }

            if (totalSize > DISK_CACHE_MAX_BYTES) {
                var sizeToFree = totalSize - (DISK_CACHE_MAX_BYTES * 0.8).toLong()
                for (f in validFiles) {
                    if (sizeToFree <= 0) break
                    sizeToFree -= f.length()
                    f.delete()
                }
                log("磁盘缓存清理完成，释放空间")
            }
        } catch (_: Throwable) {
        }
    }

    private fun deliverResult(
        iv: ImageView,
        bitmap: Bitmap?,
        isHd: Boolean,
        onResult: ((ImageView, Bitmap?, Boolean) -> Unit)?
    ) {
        if (onResult == null) {
            if (bitmap != null) iv.setImageBitmap(bitmap)
            else iv.setImageResource(R.mipmap.icon_video_placeholder)
        } else {
            onResult(iv, bitmap, isHd)
        }
    }

    fun cancel(imageView: ImageView) {
        val ivKey = viewKey(imageView)
        val taskId = viewTaskMap.remove(ivKey) ?: return
        tasks.remove(taskId)?.let { task ->
            log("[${task.id}] cancel() 取消任务")
            task.future?.let { if (!it.isDone && !it.isCancelled) it.cancel(true) }
        }
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
        failureBackoffMap.clear()
        log("内存缓存已清空")
    }

    fun clearDiskCache(context: Context) {
        executor.submit {
            try {
                getDiskCacheDir(context).listFiles()?.forEach { it.delete() }
                log("磁盘缓存已清空")
            } catch (_: Throwable) {
            }
        }
    }

    fun clearAllCache(context: Context) {
        runCatching {
            clearMemoryCache()
            clearDiskCache(context)
        }
    }

    fun getDiskCacheSize(context: Context): Long {
        return try {
            getDiskCacheDir(context).listFiles()?.sumOf { it.length() } ?: 0L
        } catch (_: Throwable) {
            0L
        }
    }

    fun shutdown() {
        val allFutures = ArrayList<Future<*>>()
        for (task in tasks.values) {
            task.future?.let { allFutures.add(it) }
        }
        allFutures.forEach { if (!it.isDone && !it.isCancelled) it.cancel(true) }
        tasks.clear()
        viewTaskMap.clear()
        failureBackoffMap.clear()
        executor.shutdownNow()
        log("shutdown() 完成, executor 已终止")
    }

    // ================= 工具方法 =================

    private fun cacheKey(url: String, frameUs: Long, quality: String): String = "$url@$frameUs@$quality"

    /**
     * 归一化 URL：
     * - 去首尾空白
     * - 仅对 http/https 的 path 段合并连续斜杠（不影响 scheme）
     * 例如 https://a.com//x///y.mp4 -> https://a.com/x/y.mp4
     */
    private fun normalizeVideoUrl(url: String): String {
        val raw = url.trim()
        val lower = raw.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return raw

        return runCatching {
            val uri = Uri.parse(raw)
            val encodedPath = uri.encodedPath ?: return raw
            val normalizedPath = encodedPath.replace(Regex("/{2,}"), "/")
            if (encodedPath == normalizedPath) {
                raw
            } else {
                uri.buildUpon().encodedPath(normalizedPath).build().toString()
            }
        }.getOrElse { raw }
    }

    private fun markFailure(key: String) {
        failureBackoffMap[key] = System.currentTimeMillis()
    }

    private fun clearFailureMark(key: String) {
        failureBackoffMap.remove(key)
    }

    private fun isInFailureBackoff(key: String): Boolean {
        val ts = failureBackoffMap[key] ?: return false
        val delta = System.currentTimeMillis() - ts
        return if (delta < FAILURE_BACKOFF_MS) {
            true
        } else {
            failureBackoffMap.remove(key, ts)
            false
        }
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun scaleDown(src: Bitmap, maxSize: Int): Bitmap {
        if (maxSize <= 0) return src
        val ratio = minOf(maxSize.toFloat() / src.width, maxSize.toFloat() / src.height)
        if (ratio >= 1f) return src
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    /**
     * 限制 Bitmap 最大尺寸，使用双线性滤波提高缩放质量
     */
    private fun limitBitmapSize(src: Bitmap, maxSize: Int): Bitmap {
        if (maxSize <= 0) return src
        val ratio = minOf(maxSize.toFloat() / src.width, maxSize.toFloat() / src.height)
        if (ratio >= 1f) return src
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        if (scaled !== src) src.recycle()
        return scaled
    }

    private fun setDataSourceSmart(
        retriever: MediaMetadataRetriever,
        appCtx: Context?,
        url: String,
        headers: Map<String, String>?
    ) {
        val safeUrl = normalizeVideoUrl(url)
        val lower = safeUrl.lowercase()
        when {
            lower.startsWith("http://") || lower.startsWith("https://") -> {
                retriever.setDataSource(safeUrl, headers ?: emptyMap())
            }
            lower.startsWith("content://") -> {
                if (appCtx != null) {
                    try {
                        retriever.setDataSource(appCtx, Uri.parse(safeUrl))
                        return
                    } catch (_: Throwable) {
                    }
                }
                retriever.setDataSource(safeUrl)
            }
            else -> {
                retriever.setDataSource(safeUrl)
            }
        }
    }

    /**
     * MediaMetadataRetriever 完全失败时的兜底方案：
     * 使用 Glide 尝试解码指定帧。仅在失败场景触发，避免影响主路径性能。
     */
    private fun extractFrameByGlide(
        appCtx: Context,
        url: String,
        frameUs: Long,
        size: Int
    ): Bitmap? {
        if (Thread.currentThread().isInterrupted) return null

        // 优先尝试 OSS 服务端截帧（不依赖设备本地视频解码能力）
        val ossSnapshotUrl = buildOssSnapshotUrl(url, frameUs, size)
        if (!ossSnapshotUrl.isNullOrBlank()) {
            log("尝试 OSS 服务端截帧: ${ossSnapshotUrl.takeLast(120)}")
            val snapshotBitmap = requestBitmapByGlide(
                appCtx = appCtx,
                model = ossSnapshotUrl,
                size = size,
                frameUs = null
            )
            if (snapshotBitmap != null) {
                log("OSS 服务端截帧成功")
                return snapshotBitmap
            }
            log("OSS 服务端截帧失败，回退到 Glide 视频抽帧")
        }

        // 再尝试 Glide 视频帧（仍依赖本地解码）
        return requestBitmapByGlide(
            appCtx = appCtx,
            model = buildGlideModel(url),
            size = size,
            frameUs = frameUs
        )
    }

    private fun buildGlideModel(url: String): Any {
        val normalized = normalizeVideoUrl(url)
        val lower = normalized.lowercase()
        return when {
            lower.startsWith("http://") || lower.startsWith("https://") -> normalized
            lower.startsWith("content://") || lower.startsWith("file://") -> Uri.parse(normalized)
            else -> {
                val file = File(normalized)
                if (file.exists()) file else normalized
            }
        }
    }

    /**
     * 构造 OSS 视频截帧 URL：
     * https://xxx.oss-*.aliyuncs.com/a.mp4?x-oss-process=video/snapshot,t_1000,f_jpg,w_720,h_720,m_fast
     */
    private fun buildOssSnapshotUrl(url: String, frameUs: Long, size: Int): String? {
        val normalized = normalizeVideoUrl(url)
        val lower = normalized.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return null

        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        if (!host.contains("aliyuncs.com", ignoreCase = true)) return null

        val pathLower = (uri.path ?: "").lowercase()
        if (VIDEO_SUFFIXES.none { pathLower.endsWith(it) }) return null

        val snapshotMs = (frameUs / 1000L).coerceAtLeast(1_000L)
        val targetSize = size.coerceAtLeast(1)
        val process = "video/snapshot,t_${snapshotMs},f_jpg,w_${targetSize},h_${targetSize},m_fast"

        val base = uri.buildUpon().encodedQuery(null).fragment(null).build().toString()
        val encodedQuery = uri.encodedQuery
        val preserved = if (encodedQuery.isNullOrBlank()) {
            emptyList()
        } else {
            encodedQuery.split("&")
                .filter { it.isNotBlank() && !it.startsWith("x-oss-process=", ignoreCase = true) }
        }
        val finalQuery = (preserved + "x-oss-process=$process").joinToString("&")
        val fragment = uri.encodedFragment?.let { "#$it" } ?: ""
        return "$base?$finalQuery$fragment"
    }

    private fun requestBitmapByGlide(
        appCtx: Context,
        model: Any,
        size: Int,
        frameUs: Long?
    ): Bitmap? {
        if (Thread.currentThread().isInterrupted) return null

        val options = RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565)
            .downsample(DownsampleStrategy.AT_MOST)
            .timeout(GLIDE_FALLBACK_TIMEOUT_MS)
            .override(size, size)
        if (frameUs != null) {
            options.frame(frameUs)
        }

        val target = Glide.with(appCtx)
            .asBitmap()
            .load(model)
            .apply(options)
            .submit(size, size)

        return try {
            target.get(GLIDE_FALLBACK_WAIT_SECONDS, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log("Glide 兜底被中断")
            null
        } catch (e: Throwable) {
            log("Glide 兜底失败: ${e.message}")
            null
        } finally {
            target.cancel(true)
            mainHandler.post {
                runCatching { Glide.with(appCtx).clear(target) }
            }
        }
    }
}
