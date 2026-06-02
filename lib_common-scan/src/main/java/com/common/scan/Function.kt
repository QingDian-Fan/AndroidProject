package com.common.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.common.scan.camera.util.LogUtils


/**
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
fun Bitmap.drawRect(block: (canvas: Canvas, paint: Paint) -> Unit): Bitmap {
    val result = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    try {
        val canvas = Canvas(result)
        canvas.drawBitmap(this, 0f, 0f, null)
        val paint = Paint()
        paint.strokeWidth = 6f
        paint.style = Paint.Style.STROKE
        paint.color = Color.RED

        block(canvas, paint)

        canvas.save()
        canvas.restore()
    } catch (e: Exception) {
        LogUtils.w(e.message)
    }
    return result
}

/**
 * 通过uri获取 Bitmap
 */
fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return context.contentResolver.openInputStream(uri)?.use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }
}
