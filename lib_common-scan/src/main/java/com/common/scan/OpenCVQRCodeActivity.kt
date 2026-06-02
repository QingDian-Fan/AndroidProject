package com.common.scan

import android.content.Intent
import android.graphics.Path

import com.common.scan.camera.AnalyzeResult
import com.common.scan.camera.CameraScan
import com.common.scan.camera.analyze.Analyzer

import com.common.scan.opencv.OpenCVCameraScanActivity
import com.common.scan.opencv.analyze.OpenCVScanningAnalyzer
import com.common.scan.R

/**
 * OpenCV二维码扫描实现示例
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <p>
 * <a href="https://github.com/jenly1314">Follow me</a>
 */
class OpenCVQRCodeActivity : OpenCVCameraScanActivity() {

    override fun onScanResultCallback(result: AnalyzeResult<MutableList<String>>) {
        // 停止分析
        cameraScan.setAnalyzeImage(false)

        // 当初始化 OpenCVScanningAnalyzer 时，如果是需要二维码的位置信息，则可通过 OpenCVAnalyzeResult 获取
        if (result is OpenCVScanningAnalyzer.QRCodeAnalyzeResult) { // 如果需要处理结果二维码的位置信息
            val buffer = StringBuilder()
            val bitmap = result.bitmap!!.drawRect { canvas, paint ->
                // 扫码结果
                result.result.forEachIndexed { index, data ->
                    buffer.append("[$index] ").append(data).append("\n")
                }

                for (i in 0 until result.points.rows()) {
                    result.points.row(i).let { mat ->

                        val path = Path()
                        path.moveTo(mat[0, 0][0].toFloat(), mat[0, 0][1].toFloat())
                        path.lineTo(mat[0, 1][0].toFloat(), mat[0, 1][1].toFloat())
                        path.lineTo(mat[0, 2][0].toFloat(), mat[0, 2][1].toFloat())
                        path.lineTo(mat[0, 3][0].toFloat(), mat[0, 3][1].toFloat())
                        path.lineTo(mat[0, 0][0].toFloat(), mat[0, 0][1].toFloat())
                        // 将二维码位置在图片上框出来
                        canvas.drawPath(path, paint)
                    }
                }
            }

          /*  val config = AppDialogConfig(this, R.layout.qrcode_result_dialog).apply {
                content = buffer
                onClickConfirm = View.OnClickListener {
                    AppDialog.dismissDialog()
                    // 继续扫码分析
                    cameraScan.setAnalyzeImage(true)
                }
                onClickCancel = View.OnClickListener {
                    AppDialog.dismissDialog()
                    finish()
                }
                viewHolder.setImageBitmap(R.id.ivDialogContent, bitmap)
            }
            AppDialog.showDialog(config, false)*/

        } else {
            // 一般需求都是识别一个码，所以这里取第0个就可以；有识别多个码的需求，可以取全部
            val text = result.result[0]
            val intent = Intent()
            intent.putExtra(CameraScan.SCAN_RESULT, text)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun createAnalyzer(): Analyzer<MutableList<String>> {
        // 如果需要返回结果二维码位置信息，则初始化分析器时，isOutputVertices参数传 true 即可
        return OpenCVScanningAnalyzer(true)
    }

}
