@file:Suppress("DEPRECATION")

package com.common.utils

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.text.TextUtils
import android.view.WindowManager
import java.util.concurrent.Executors

class ScreenShotListenManager(private val context: Context) {

    /**
     * 读取媒体数据库时需要读取的列, 其中 WIDTH 和 HEIGHT 字段在 API 16 以后才有
     */
    private val mediaProjections = arrayOf(
        MediaStore.Images.ImageColumns.DATA,
        MediaStore.Images.ImageColumns.DATE_TAKEN,
        MediaStore.Images.ImageColumns.WIDTH,
        MediaStore.Images.ImageColumns.HEIGHT
    )

    /**
     * 截屏依据中的路径判断关键字
     */
    private val keywords = arrayOf(
        "screenshot", "screen_shot", "screen-shot", "screen shot",
        "screencapture", "screen_capture", "screen-capture", "screen capture",
        "screencap", "screen_cap", "screen-cap", "screen cap"
    )

    private var screenRealSize: Point? = null

    /**
     * 已回调过的路径
     */
    private val hasCallbackPaths = ArrayList<String>()

    private var listener: OnScreenShotListener? = null

    private var startListenTime: Long = 0

    /**
     * 内部存储器内容观察者
     */
    private var internalObserver: MediaContentObserver? = null

    /**
     * 外部存储器内容观察者
     */
    private var externalObserver: MediaContentObserver? = null

    init {
        // 获取屏幕真实的分辨率
        if (screenRealSize == null) {
            screenRealSize = getRealScreenSize()
        }
    }

    /**
     * 启动监听
     */
    fun startListen() {
        hasCallbackPaths.clear()

        // 记录开始监听的时间戳
        startListenTime = System.currentTimeMillis()
        // 创建内容观察者
        internalObserver = MediaContentObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI, null)
        externalObserver = MediaContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)

        // 注册内容观察者
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.INTERNAL_CONTENT_URI, true, internalObserver!!
        )
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, externalObserver!!
        )
    }

    /**
     * 停止监听
     */
    fun stopListen() {
        // 注销内容观察者
        internalObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            internalObserver = null
        }
        externalObserver?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            externalObserver = null
        }

        // 清空数据
        startListenTime = 0
        hasCallbackPaths.clear()
    }

    /**
     * 处理媒体数据库的内容改变
     */
    private fun handleMediaContentChange(contentUri: Uri): ScreenData? {
        var cursor: android.database.Cursor? = null
        try {
            // 数据改变时查询数据库中最后加入的一条数据
            cursor = if (Build.VERSION.SDK_INT >= 26) {
                // Android R 条件限制需要使用 bundle 否则报：java.lang.IllegalArgumentException: Invalid token limit
                val bundle = Bundle()
                val array = arrayOf(MediaStore.Images.ImageColumns.DATE_ADDED)
                // 按照该列 倒叙 取1条
                bundle.putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, array)
                bundle.putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
                context.contentResolver.query(contentUri, mediaProjections, bundle, null)
            } else {
                context.contentResolver.query(
                    contentUri,
                    mediaProjections,
                    null,
                    null,
                    MediaStore.Images.ImageColumns.DATE_ADDED + " desc limit 1"
                )
            }

            if (cursor == null) {
                return null
            }
            if (!cursor.moveToFirst()) {
                return null
            }

            // 获取各列的索引
            val dataIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            val dateTakenIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val widthIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.WIDTH)
            val heightIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.HEIGHT)

            // 获取行数据
            val data = cursor.getString(dataIndex)
            val dateTaken = cursor.getLong(dateTakenIndex)
            val width = cursor.getInt(widthIndex)
            val height = cursor.getInt(heightIndex)
            return ScreenData(data, dateTaken, width, height)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            if (cursor != null && !cursor.isClosed) {
                cursor.close()
            }
        }
    }

    /**
     * 处理获取到的一行数据
     */
    private fun handleMediaRowData(screenData: ScreenData) {
        if (checkScreenShot(screenData)) {
            val data = screenData.data
            if (listener != null && data != null && !checkCallback(data)) {
                listener?.onShot(data)
            }
        }
    }

    /**
     * 判断指定的数据行是否符合截屏条件
     */
    private fun checkScreenShot(screenData: ScreenData): Boolean {
        // 判断依据一: 时间判断
        // 如果加入数据库的时间在开始监听之前, 或者与当前时间相差大于10秒, 则认为当前没有截屏
        if (screenData.dateTaken < startListenTime ||
            (System.currentTimeMillis() - screenData.dateTaken) > 10 * 1000
        ) {
            return false
        }

        // 判断依据二: 尺寸判断
        screenRealSize?.let { size ->
            // 如果图片尺寸超出屏幕, 则认为当前没有截屏
            if (!((screenData.width <= size.x && screenData.height <= size.y) ||
                    (screenData.height <= size.x && screenData.width <= size.y))
            ) {
                return false
            }
        }

        // 判断依据三: 路径判断
        var data = screenData.data
        if (TextUtils.isEmpty(data)) {
            return false
        }
        data = data!!.lowercase()
        // 判断图片路径是否含有指定的关键字之一, 如果有, 则认为当前截屏了
        for (keyWork in keywords) {
            if (data.contains(keyWork)) {
                return true
            }
        }
        return false
    }

    /**
     * 判断是否已回调过, 某些手机ROM截屏一次会发出多次内容改变的通知; <br></br>
     * 删除一个图片也会发通知, 同时防止删除图片时误将上一张符合截屏规则的图片当做是当前截屏.
     */
    private fun checkCallback(imagePath: String): Boolean {
        if (hasCallbackPaths.contains(imagePath)) {
            return true
        }
        // 大概缓存15~20条记录便可
        if (hasCallbackPaths.size >= 20) {
            for (i in 0 until 5) {
                hasCallbackPaths.removeAt(0)
            }
        }
        hasCallbackPaths.add(imagePath)
        return false
    }

    /**
     * 获取屏幕分辨率
     */
    private fun getRealScreenSize(): Point? {
        var screenSize: Point? = null
        try {
            screenSize = Point()
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val defaultDisplay = windowManager.defaultDisplay
            defaultDisplay.getRealSize(screenSize)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return screenSize
    }

    /**
     * 设置截屏监听器
     */
    fun setListener(listener: OnScreenShotListener?) {
        this.listener = listener
    }

    fun interface OnScreenShotListener {
        fun onShot(imagePath: String)
    }

    /**
     * 媒体内容观察者(观察媒体数据库的改变)
     */
    private inner class MediaContentObserver(
        private val contentUri: Uri,
        handler: Handler?
    ) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            handleScreenData(contentUri)
        }
    }

    /**
     * TODO 待解决：有些场景 多次 回调 多次创建线程
     * 改用本地的线程池
     */
    private fun handleScreenData(contentUri: Uri) {
        val workService = Executors.newCachedThreadPool()
        workService.execute {
            val screenData = handleMediaContentChange(contentUri)
            if (screenData == null) {
                LogUtil.e(TAG, "--------------handleScreenData:获取截图失败")
            } else {
                handleMediaRowData(screenData)
            }
        }
    }

    private class ScreenData(
        val data: String?,
        val dateTaken: Long,
        val width: Int,
        val height: Int
    )

    companion object {
        private const val TAG = "ScreenShotListenManager"
    }
}