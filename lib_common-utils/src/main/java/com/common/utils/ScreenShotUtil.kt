@file:Suppress("DEPRECATION")

package com.common.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.LruCache
import android.view.View
import android.webkit.WebView
import android.widget.ListView
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView

object ScreenShotUtil {

    /**
     * 根据指定的Activity截图（带空白的状态栏）
     *
     * @param context 要截图的Activity
     * @return Bitmap
     */
    @JvmStatic
    fun shotActivity(context: Activity): Bitmap {
        val view = context.window.decorView
        view.isDrawingCacheEnabled = true
        view.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(view.drawingCache, 0, 0, view.measuredWidth, view.measuredHeight)
        view.isDrawingCacheEnabled = false
        view.destroyDrawingCache()
        return bitmap
    }

    /**
     * 根据指定的Activity截图（去除状态栏）
     *
     * @param activity 要截图的Activity
     * @return Bitmap
     */
    @JvmStatic
    fun shotActivityNoStatusBar(activity: Activity): Bitmap {
        // 获取windows中最顶层的view
        val view = activity.window.decorView
        view.buildDrawingCache()
        // 获取状态栏高度
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        val statusBarHeights = rect.top
        val display = activity.windowManager.defaultDisplay
        // 获取屏幕宽和高
        val widths = display.width
        val heights = display.height
        // 允许当前窗口保存缓存信息
        view.isDrawingCacheEnabled = true
        // 去掉状态栏
        val bmp = Bitmap.createBitmap(
            view.drawingCache, 0,
            statusBarHeights, widths, heights - statusBarHeights
        )
        // 销毁缓存信息
        view.destroyDrawingCache()
        return bmp
    }

    /**
     * 根据指定的view截图
     *
     * @param v 要截图的view
     * @return Bitmap
     */
    @JvmStatic
    fun getViewBitmap(v: View?): Bitmap? {
        if (v == null) {
            return null
        }
        v.isDrawingCacheEnabled = true
        v.buildDrawingCache()
        v.measure(
            View.MeasureSpec.makeMeasureSpec(v.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(v.height, View.MeasureSpec.EXACTLY)
        )
        v.layout(
            v.x.toInt(), v.y.toInt(),
            v.x.toInt() + v.measuredWidth, v.y.toInt() + v.measuredHeight
        )
        val bitmap = Bitmap.createBitmap(v.drawingCache, 0, 0, v.measuredWidth, v.measuredHeight)
        v.isDrawingCacheEnabled = false
        v.destroyDrawingCache()
        return bitmap
    }

    /**
     * Scrollview截屏
     *
     * @param scrollView 要截图的ScrollView
     * @return Bitmap
     */
    @JvmStatic
    fun shotScrollView(scrollView: ScrollView): Bitmap {
        var h = 0
        for (i in 0 until scrollView.childCount) {
            h += scrollView.getChildAt(i).height
            scrollView.getChildAt(i).setBackgroundColor(Color.parseColor("#ffffff"))
        }
        val bitmap = Bitmap.createBitmap(scrollView.width, h, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        scrollView.draw(canvas)
        return bitmap
    }

    /**
     * ListView截图
     *
     * @param listView 要截图的ListView
     * @return Bitmap
     */
    @JvmStatic
    fun shotListView(listView: ListView): Bitmap {
        val adapter = listView.adapter
        val itemsCount = adapter.count
        var allItemsHeight = 0

        val bmps = ArrayList<Bitmap>()
        for (i in 0 until itemsCount) {
            val childView = adapter.getView(i, null, listView)
            childView.measure(
                View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            childView.layout(0, 0, childView.measuredWidth, childView.measuredHeight)
            childView.isDrawingCacheEnabled = true
            childView.buildDrawingCache()
            bmps.add(childView.drawingCache)
            allItemsHeight += childView.measuredHeight
        }

        val bigBitmap = Bitmap.createBitmap(listView.measuredWidth, allItemsHeight, Bitmap.Config.ARGB_8888)
        val bigCanvas = Canvas(bigBitmap)
        val paint = Paint()
        var iHeight = 0
        for (i in bmps.indices) {
            val bmp = bmps[i]
            bigCanvas.drawBitmap(bmp, 0f, iHeight.toFloat(), paint)
            iHeight += bmp.height
            bmp.recycle()
        }
        return bigBitmap
    }

    /**
     * RecyclerView截屏
     *
     * @param view 要截图的RecyclerView
     * @return Bitmap
     */
    @JvmStatic
    fun shotRecyclerView(view: RecyclerView): Bitmap? {
        @Suppress("UNCHECKED_CAST")
        val adapter = view.adapter as? RecyclerView.Adapter<RecyclerView.ViewHolder>
        var bigBitmap: Bitmap? = null
        if (adapter != null) {
            val size = adapter.itemCount
            var height = 0
            val paint = Paint()
            var iHeight = 0
            val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
            // Use 1/8th of the available memory for this memory cache.
            val cacheSize = maxMemory / 8
            val bitmapCache = LruCache<String, Bitmap>(cacheSize)
            for (i in 0 until size) {
                val holder = adapter.createViewHolder(view, adapter.getItemViewType(i))
                adapter.onBindViewHolder(holder, i)
                holder.itemView.measure(
                    View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                holder.itemView.layout(
                    0, 0, holder.itemView.measuredWidth,
                    holder.itemView.measuredHeight
                )
                holder.itemView.isDrawingCacheEnabled = true
                holder.itemView.buildDrawingCache()
                val drawingCache = holder.itemView.drawingCache
                if (drawingCache != null) {
                    bitmapCache.put(i.toString(), drawingCache)
                }
                height += holder.itemView.measuredHeight
            }

            bigBitmap = Bitmap.createBitmap(view.measuredWidth, height, Bitmap.Config.ARGB_8888)
            val bigCanvas = Canvas(bigBitmap)
            val lBackground = view.background
            if (lBackground is ColorDrawable) {
                bigCanvas.drawColor(lBackground.color)
            }
            for (i in 0 until size) {
                val bitmap = bitmapCache.get(i.toString())
                bigCanvas.drawBitmap(bitmap, 0f, iHeight.toFloat(), paint)
                iHeight += bitmap.height
                bitmap.recycle()
            }
        }
        return bigBitmap
    }

    /**
     * 截取webView可视区域的截图
     *
     * @param webView 前提：WebView要设置webView.setDrawingCacheEnabled(true);
     */
    private fun captureWebViewVisibleSize(webView: WebView): Bitmap = webView.drawingCache

    /**
     * 截取webView快照(webView加载的整个内容的大小)
     */
    private fun captureWebView(webView: WebView): Bitmap {
        val snapShot: Picture = webView.capturePicture()
        val bmp = Bitmap.createBitmap(snapShot.width, snapShot.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        snapShot.draw(canvas)
        return bmp
    }

    /**
     * 截屏
     */
    private fun captureScreen(context: Activity): Bitmap {
        val cv = context.window.decorView
        val bmp = Bitmap.createBitmap(cv.width, cv.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        cv.draw(canvas)
        return bmp
    }
}