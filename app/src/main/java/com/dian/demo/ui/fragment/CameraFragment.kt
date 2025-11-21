package com.dian.demo.ui.fragment

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.MotionEvent

import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dian.demo.ProjectApplication
import com.dian.demo.R
import com.dian.demo.base.BaseAppBindFragment
import com.dian.demo.databinding.FragmentCameraBinding
import com.dian.demo.ui.view.CameraButtonView
import com.dian.demo.ui.view.FocusCornerDrawable
import com.dian.demo.utils.aop.CheckPermissions
import com.dian.demo.utils.permissions.DefaultPermissionInterceptor
import com.dian.demo.utils.permissions.LivePermissions
import com.dian.demo.utils.permissions.PermissionResult
import java.io.File
import java.util.concurrent.TimeUnit


class CameraFragment : BaseAppBindFragment<FragmentCameraBinding>() {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var camera: Camera? = null

    private var timer: CountDownTimer? = null
    private var elapsedTime = 0


    override fun getLayoutId(): Int = R.layout.fragment_camera

    override fun initialize(savedInstanceState: Bundle?) {
        startCamera()
        setupCaptureButton()
        setupSwitchCameraButton()
    }

    private fun setupCaptureButton() {
        // binding.captureButton.setButtonType(CameraButtonView.ButtonType.TOGGLE_CLICK)
        binding.captureButton.setCameraButtonCallback(object :
            CameraButtonView.CameraButtonCallback {
            override fun onTakePhoto() {
                takePhoto()
            }

            override fun onStartRecord() {
                startRecording()
            }

            override fun onEndRecord() {
                stopRecording()
            }

        })
    }

    private fun setupSwitchCameraButton() {
        binding.btnFlip.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
    }

    @CheckPermissions(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        isMust = true
    )
    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(ProjectApplication.getAppContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            try {
                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

                // ✅ 启用双指缩放
                enableTouchControls()

            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(ProjectApplication.getAppContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val mediaDir = ProjectApplication.getAppInstance().externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraXPhotos").apply { mkdirs() }
        } ?: ProjectApplication.getAppContext().filesDir

        val photoFile = File(mediaDir, "${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(ProjectApplication.getAppInstance()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    savedUri.path?.let {
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(File(it).absolutePath),
                            arrayOf("image/jpeg"),
                            null
                        )
                    }
                    Toast.makeText(
                        ProjectApplication.getAppContext(),
                        "📸 Photo saved: ${photoFile.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun startRecording() {
        val videoCapture = videoCapture ?: return

        val mediaDir = ProjectApplication.getAppContext().externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraXVideos").apply { mkdirs() }
        } ?: ProjectApplication.getAppContext().filesDir

        val videoFile = File(mediaDir, "${System.currentTimeMillis()}.mp4")
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        recording = videoCapture.output
            .prepareRecording(ProjectApplication.getAppContext(), outputOptions)
            .apply {
                if (ActivityCompat.checkSelfPermission(
                        ProjectApplication.getAppContext(), Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(ProjectApplication.getAppContext())) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        binding.recordTimer.visibility = View.VISIBLE
                        startTimer()
                    }

                    is VideoRecordEvent.Finalize -> {
                        binding.recordTimer.visibility = View.GONE
                        stopTimer()
                        if (!recordEvent.hasError()) {
                            MediaScannerConnection.scanFile(
                                context,
                                arrayOf(videoFile.absolutePath),
                                arrayOf("video/mp4"),
                                null
                            )
                            Toast.makeText(
                                ProjectApplication.getAppContext(),
                                "🎬 Video saved: ${videoFile.absolutePath}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            videoFile.delete()
                        }
                    }
                }
            }
    }

    private fun stopRecording() {
        recording?.stop()
        recording = null
    }


    private fun startTimer() {
        elapsedTime = 0
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTime++
                val minutes = elapsedTime / 60
                val seconds = elapsedTime % 60
                binding.recordTimer.text = String.format("%02d:%02d", minutes, seconds)
            }
            override fun onFinish() {}
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        binding.recordTimer.text = "00:00"
    }


    private fun enableTouchControls() {
        var isZooming = false

        val scaleGestureDetector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                    isZooming = true
                    return true
                }

                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val cameraControl = camera?.cameraControl ?: return false
                    val cameraInfo = camera?.cameraInfo ?: return false
                    val currentZoom = cameraInfo.zoomState.value?.zoomRatio ?: 1f
                    val newZoom = (currentZoom * detector.scaleFactor).coerceIn(1f, 10f)
                    cameraControl.setZoomRatio(newZoom)
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) {
                    // 放大操作结束，允许再次点击对焦
                    isZooming = false
                }
            })

        binding.previewView.setOnTouchListener { view, event ->
            scaleGestureDetector.onTouchEvent(event)
            if (!isZooming && event.pointerCount == 1 && event.action == MotionEvent.ACTION_UP) {
                val factory = binding.previewView.meteringPointFactory
                val point = factory.createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(point)
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()
                camera?.cameraControl?.startFocusAndMetering(action)
                showTapFocusAnimation(event.x, event.y, view as PreviewView)
            }
            true
        }
    }


    private fun showTapFocusAnimation(x: Float, y: Float, previewView: PreviewView) {
        val focusView = View(previewView.context).apply {
            layoutParams = FrameLayout.LayoutParams(100, 100)
            background = FocusCornerDrawable(Color.YELLOW, 4f, 30f)
            translationX = x - 50
            translationY = y - 50
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
        }

        (previewView.parent as ViewGroup).addView(focusView)

        focusView.animate()
            .scaleX(0.6f)
            .scaleY(0.6f)
            .alpha(0f)
            .setDuration(600)
            .withEndAction { (previewView.parent as ViewGroup).removeView(focusView) }
            .start()
    }

    companion object {

        @JvmStatic
        fun getFragment() = CameraFragment()
    }
}