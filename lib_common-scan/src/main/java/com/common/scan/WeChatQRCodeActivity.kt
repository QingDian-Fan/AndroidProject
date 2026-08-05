package com.common.scan

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.common.scan.camera.AnalyzeResult
import com.common.scan.camera.CameraScan
import com.common.scan.camera.analyze.Analyzer
import com.common.scan.camera.util.PointUtils
import com.common.scan.view.ActionIconView
import com.common.scan.wechat.WeChatCameraScanActivity
import com.common.scan.wechat.WeChatQRCodeDetector
import com.common.scan.wechat.analyze.WeChatScanningAnalyzer
import org.opencv.core.Mat
import java.util.concurrent.Executors

/**
 * 微信二维码扫描实现示例
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class WeChatQRCodeActivity : WeChatCameraScanActivity() {

    private lateinit var ivResult: ImageView
    private lateinit var ivClose: ActionIconView
    private lateinit var ivAlbum: ActionIconView
    private val albumExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var albumResultBitmap: Bitmap? = null

    companion object {
        private const val REQUEST_SELECT_IMAGE = 1001
        private const val MAX_DECODE_SIZE = 2048
    }

    override fun initUI() {
        super.initUI()
        ivResult = findViewById(R.id.ivResult)
        ivClose = findViewById(R.id.iv_close)
        ivAlbum = findViewById(R.id.iv_album)
        ivClose.setOnClickListener { onBackPressedDispatcher.onBackPressed()}
        ivAlbum.setOnClickListener {
            openAlbum()
        }
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 如果是结果点显示时，用户点击了返回键，则认为是取消选择当前结果，重新开始扫码
                if (viewfinderView.isShowPoints) {
                    resumeCameraScan()
                } else {
                    finish()
                }
            }

        })
    }

    private fun handleAlbumResult(bitmap: Bitmap, results: List<String>, centers: List<Point>) {
        when {
            results.isEmpty() -> {
                bitmap.recycle()
                showAlbumError(R.string.scan_album_qr_not_found)
            }
            results.size == 1 || centers.size != results.size -> {
                bitmap.recycle()
                returnScanResult(results.first())
            }
            else -> showAlbumResults(bitmap, results, centers)
        }
    }

    private fun showAlbumResults(bitmap: Bitmap, results: List<String>, centers: List<Point>) {
        clearResultImage()
        albumResultBitmap = bitmap
        ivResult.setImageBitmap(bitmap)
        val displayPoints = centers.map { point ->
            PointUtils.transform(
                point,
                bitmap.width,
                bitmap.height,
                viewfinderView.width,
                viewfinderView.height,
                true
            )
        }
        viewfinderView.setOnItemClickListener { returnScanResult(results[it]) }
        viewfinderView.showResultPoints(displayPoints)
    }

    private fun decodeSampledBitmap(uri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r") ?: return null
            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: Exception) {
           e.printStackTrace()
        }
        return null
    }

    private fun showAlbumError(messageRes: Int) {
        if (isFinishing || isDestroyed) return
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        resumeCameraScan()
    }

    private fun resumeCameraScan() {
        clearResultImage()
        viewfinderView.showScanner()
        ivAlbum.isEnabled = true
        cameraScan.setAnalyzeImage(true)
    }

    private fun clearResultImage() {
        ivResult.setImageDrawable(null)
        albumResultBitmap?.recycle()
        albumResultBitmap = null
    }

    private fun returnScanResult(result: String) {
        setResult(RESULT_OK, Intent().putExtra(CameraScan.SCAN_RESULT, result))
        finish()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        albumExecutor.shutdownNow()
        clearResultImage()
        super.onDestroy()
    }

    override fun onScanResultCallback(result: AnalyzeResult<List<String>>) {
        // 停止分析
        cameraScan.setAnalyzeImage(false)
        val width = result.imageWidth
        val height = result.imageHeight

        // 当初始化 WeChatScanningAnalyzer 时，如果是需要二维码的位置信息，则可通过 WeChatScanningAnalyzer.QRCodeAnalyzeResult 获取
        if (result is WeChatScanningAnalyzer.QRCodeAnalyzeResult) { // 如果需要处理结果二维码的位置信息
            //取预览当前帧图片并显示，为结果点提供参照
            ivResult.setImageBitmap(previewView.bitmap)
            val points = ArrayList<Point>()
            result.points?.forEach { mat ->
                // 扫码结果二维码的四个点（一个矩形）


                val point0 = Point(mat[0, 0][0].toInt(), mat[0, 1][0].toInt())
                val point1 = Point(mat[1, 0][0].toInt(), mat[1, 1][0].toInt())
                val point2 = Point(mat[2, 0][0].toInt(), mat[2, 1][0].toInt())
                val point3 = Point(mat[3, 0][0].toInt(), mat[3, 1][0].toInt())

                val centerX = (point0.x + point1.x + point2.x + point3.x) / 4
                val centerY = (point0.y + point1.y + point2.y + point3.y) / 4

                //将实际的结果中心点坐标转换成界面预览的坐标
                val point = PointUtils.transform(
                    centerX,
                    centerY,
                    width,
                    height,
                    viewfinderView.width,
                    viewfinderView.height
                )
                points.add(point)
            }
            //设置Item点击监听
            viewfinderView.setOnItemClickListener {
                //显示点击Item将所在位置扫码识别的结果返回
                returnScanResult(result.result[it])
            }
            //显示结果点信息
            viewfinderView.showResultPoints(points)

            if (result.result.size == 1) {
                returnScanResult(result.result[0])
            }
        } else {
            // 一般需求都是识别一个码，所以这里取第0个就可以；有识别多个码的需求，可以取全部
            returnScanResult(result.result[0])
        }
    }

    override fun createAnalyzer(): Analyzer<MutableList<String>> {
        // 分析器默认不会返回结果二维码的位置信息
//        return WeChatScanningAnalyzer()
        // 如果需要返回结果二维码位置信息，则初始化分析器时，isOutputVertices参数传 true 即可
        return WeChatScanningAnalyzer(true)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_wechat_qrcode
    }


    @Suppress("DEPRECATION")
    private fun openAlbum() {
        cameraScan.setAnalyzeImage(false)
        ivAlbum.isEnabled = false
        runCatching {
            startActivityForResult(selectIntent(), REQUEST_SELECT_IMAGE)
        }.onFailure {
            showAlbumError(R.string.scan_album_image_unavailable)
        }
    }

    @Deprecated("Deprecated in Android SDK, retained for PictureSelector compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_SELECT_IMAGE) return

        val uri = result(resultCode, data)
        if (uri == null) {
            resumeCameraScan()
        } else {
            decodeAlbumQRCode(uri)
        }
    }

    private fun decodeAlbumQRCode(uri: Uri) {
        albumExecutor.execute {
            val bitmap = runCatching { decodeSampledBitmap(uri) }.getOrNull()
            if (bitmap == null) {
                mainHandler.post { showAlbumError(R.string.scan_album_image_unavailable) }
                return@execute
            }

            val resultPoints = ArrayList<Mat>()
            val results = runCatching {
                WeChatQRCodeDetector.detectAndDecode(bitmap, resultPoints).orEmpty()
            }.getOrElse { emptyList() }
            val centers = resultPoints.mapNotNull { mat ->
                runCatching {
                    val centerX = (0 until 4).sumOf { mat[it, 0][0].toInt() } / 4
                    val centerY = (0 until 4).sumOf { mat[it, 1][0].toInt() } / 4
                    Point(centerX, centerY)
                }.getOrNull()
            }
            resultPoints.forEach { it.release() }

            mainHandler.post {
                if (isFinishing || isDestroyed) {
                    bitmap.recycle()
                    return@post
                }
                handleAlbumResult(bitmap, results, centers)
            }
        }
    }

    private fun selectIntent(): Intent {
        return Intent(Intent.ACTION_PICK).apply {
            setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*")
        }
    }


    fun result(resultCode: Int, data: Intent?): Uri? {
        if (resultCode != RESULT_OK) return null
        if (null == data) return null
        if (null == data.data) return null
        return data.data
    }
}
