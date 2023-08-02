package com.dian.demo.utils.code.generate

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.*

object GenerateCodeUtils {
    /**
     * 生成二维码图片大小
     */
    private var QRCODE_SIZE = 300

    /**
     * 头像图片大小
     */
    private var PORTRAIT_SIZE = 55


    /**
     * 功能:创建QR二维码图片
     * 可设置图片大小和头像图片大小
     *
     * @param portrait 头像bitmap
     * @param content  生成二维码内容数据
     */
    fun createQRCodeBitmap(
        content: String?,
        portrait: Bitmap?,
        codeSize: Int,
        logoSize: Int
    ): Bitmap? {
        QRCODE_SIZE = codeSize
        PORTRAIT_SIZE = logoSize
        // 用于设置QR二维码参数
        val qrParam = Hashtable<EncodeHintType, Any?>()
        // 设置QR二维码的纠错级别——这里选择最高H级别
        qrParam[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        // 设置编码方式
        qrParam[EncodeHintType.CHARACTER_SET] = "UTF-8"

        // 生成QR二维码数据——这里只是得到一个由true和false组成的数组
        // 参数顺序分别为：编码内容，编码类型，生成图片宽度，生成图片高度，设置参数
        try {
            var bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE, qrParam
            )

            // 开始利用二维码数据创建Bitmap图片，分别设为黑白两色
            bitMatrix = deleteWhite(bitMatrix)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val data = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    if (bitMatrix[x, y]) data[y * w + x] = -0x1000000 // 黑色
                    else data[y * w + x] = 0x00ffffff // -1 相当于0xffffffff 白色
                }
            }

            // 创建一张bitmap图片，采用最高的图片效果ARGB_8888
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            // 将上面的二维码颜色数组传入，生成图片颜色
            bitmap.setPixels(data, 0, w, 0, 0, w, h)
            if (portrait != null) {
                createQRCodeBitmapWithLogo(bitmap, initLogo(portrait))
            }
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 功能:创建QR二维码图片
     * 头像图片大小默认
     *
     * @param logo    头像bitmap
     * @param content 生成二维码内容数据
     */
    fun createQRCodeBitmap(content: String?, logo: Bitmap?): Bitmap? {
        // 用于设置QR二维码参数
        val qrParam = Hashtable<EncodeHintType, Any?>()
        // 设置QR二维码的纠错级别——这里选择最高H级别
        qrParam[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
        // 设置编码方式
        qrParam[EncodeHintType.CHARACTER_SET] = "UTF-8"

        // 生成QR二维码数据——这里只是得到一个由true和false组成的数组
        // 参数顺序分别为：编码内容，编码类型，生成图片宽度，生成图片高度，设置参数
        try {
            val bitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE, qrParam
            )

            // 开始利用二维码数据创建Bitmap图片，分别设为黑白两色
            val w = bitMatrix.width
            val h = bitMatrix.height
            val data = IntArray(w * h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    if (bitMatrix[x, y]) data[y * w + x] = -0x1000000 // 黑色
                    else data[y * w + x] = 0x00ffffff // -1 相当于0xffffffff 白色
                }
            }

            // 创建一张bitmap图片，采用最高的图片效果ARGB_8888
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            // 将上面的二维码颜色数组传入，生成图片颜色
            bitmap.setPixels(data, 0, w, 0, 0, w, h)
            if (logo != null) { //添加最中间的logo
                createQRCodeBitmapWithLogo(bitmap, initLogo(logo))
            }
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * 初始化头像图片
     */
    fun initLogo(portrait: Bitmap): Bitmap {
        // 对原有图片压缩显示大小
        val mMatrix = Matrix()
        val width = portrait.width.toFloat()
        val height = portrait.height.toFloat()
        mMatrix.setScale(PORTRAIT_SIZE / width, PORTRAIT_SIZE / height)
        return Bitmap.createBitmap(
            portrait, 0, 0, width.toInt(),
            height.toInt(), mMatrix, true
        )
    }

    /**
     * 在二维码上绘制头像
     */
    fun createQRCodeBitmapWithLogo(qr: Bitmap, portrait: Bitmap) {
        // 头像图片的大小
        val portrait_W = portrait.width
        val portrait_H = portrait.height

        // 设置头像要显示的位置，即居中显示
        val left = (qr.width - portrait_W) / 2
        val top = (qr.height - portrait_H) / 2
        val right = left + portrait_W
        val bottom = top + portrait_H
        val rect1 = Rect(left, top, right, bottom)

        // 取得qr二维码图片上的画笔，即要在二维码图片上绘制我们的头像
        val canvas = Canvas(qr)

        // 设置我们要绘制的范围大小，也就是头像的大小范围
        val rect2 = Rect(0, 0, portrait_W, portrait_H)
        // 开始绘制
        canvas.drawBitmap(portrait, rect2, rect1, null)
    }

    private fun deleteWhite(matrix: BitMatrix): BitMatrix {
        val rec = matrix.enclosingRectangle
        val resWidth = rec[2] + 1
        val resHeight = rec[3] + 1
        val resMatrix = BitMatrix(resWidth, resHeight)
        resMatrix.clear()
        for (i in 0 until resWidth) {
            for (j in 0 until resHeight) {
                if (matrix[i + rec[0], j + rec[1]]) resMatrix[i] = j
            }
        }
        return resMatrix
    }
}