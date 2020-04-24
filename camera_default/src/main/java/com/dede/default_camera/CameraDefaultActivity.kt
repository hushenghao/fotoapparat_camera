package com.dede.default_camera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.dede.default_camera.view.CaptureListener
import com.dede.default_camera.view.TypeListener
import com.dede.fotoapparat_extend.recorder.Video
import com.dede.fotoapparat_extend.recorder.VideoTaker
import com.dede.fotoapparat_extend.recorder.takeVideo
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.fileLogger
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import kotlinx.android.synthetic.main.activity_camera_default.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max

/**
 * Created by hsh on 2020/4/24 2:58 PM
 */
class CameraDefaultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CAPTURE_OUTPUT_PATH = "capture_output_path"
        const val EXTRA_RECORDER_OUTPUT_PATH = "recorder_output_path"
        const val EXTRA_RECORDER_MAX_DURATION = "recorder_max_duration"
        const val EXTRA_BUTTON_TYPE = "button_type"

        const val BUTTON_STATUS_ONLY_CAPTURE = 2
        const val BUTTON_STATUS_ONLY_RECORDER = 1
        const val BUTTON_STATUS_ALL = 0
    }

    private var isFront = false
    private lateinit var fotoapparat: Fotoapparat

    private val capturePath by lazy { intent.getStringExtra(EXTRA_CAPTURE_OUTPUT_PATH) }
    private val recorderPath by lazy { intent.getStringExtra(EXTRA_RECORDER_OUTPUT_PATH) }
    private lateinit var captureFile: File
    private lateinit var recorderFile: File

    private var maxDuration = 30 * 1000

    private val videoTaker: VideoTaker by lazy {
        recorderFile = if (TextUtils.isEmpty(recorderPath)) {
            PathGetter.generaRecorderPath(this)
        } else {
            File(recorderPath)
        }
        fotoapparat.takeVideo()
            .saveToFile(recorderFile)
            .maxDuration(maxDuration)
            .takeVideo(this::onTakeVideo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_camera_default)

        fotoapparat = Fotoapparat(
            context = this,
            focusView = focus_view,
            view = camera_view,                   // view which will draw the camera preview
            scaleType = ScaleType.CenterCrop,    // (optional) we want the preview to fill the view
            lensPosition = back(),               // (optional) we want back camera
            cameraConfiguration = CameraConfiguration.default(), // (optional) define an advanced configuration
            logger = loggers(                    // (optional) we want to log camera events in 2 places at once
                logcat(),                   // ... in logcat
                fileLogger(this)            // ... and to file
            ),
            cameraErrorCallback = { error ->
                error.printStackTrace()
            }   // (optional) log fatal errors
        )

        val status = intent.getIntExtra(EXTRA_BUTTON_TYPE, BUTTON_STATUS_ALL)
        maxDuration = intent.getIntExtra(EXTRA_RECORDER_MAX_DURATION, maxDuration)
        capture_layout.setButtonFeatures(status)
        capture_layout.setDuration(maxDuration)
        initEvent()
    }

    private fun onTakeVideo(video: Video?) {
        if (video == null) return
        iv_switch.visibility = View.GONE
        video_preview.visibility = View.VISIBLE
        video_preview.setOnPreparedListener {
            video_preview.start()
        }
        video_preview.setVideoURI(Uri.fromFile(video.sourceFile))
        fotoapparat.stop()
    }

    private fun onTakePic(unit: Unit?) {
        photo_preview.visibility = View.VISIBLE
        iv_switch.visibility = View.GONE
        val transformationBitmap = transformationBitmap(captureFile)
        photo_preview.setImageBitmap(transformationBitmap)
        fotoapparat.stop()
    }

    private fun transformationBitmap(sourceFile: File): Bitmap? {
        if (!sourceFile.exists()) {
            return null
        }
        val sourcePath = sourceFile.absolutePath
        val exifInterface = ExifInterface(sourcePath)
        val orientationCode = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val rotation = when (orientationCode) {
            ExifInterface.ORIENTATION_NORMAL -> 0
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(sourcePath, options)
        var width = options.outWidth
        var height = options.outHeight

        val targetHeight = resources.displayMetrics.heightPixels
        val targetWidth = resources.displayMetrics.widthPixels
        if (width > targetWidth && height > targetHeight) {
            // 压缩Bitmap
            val hr = height / targetHeight
            val wr = width / targetWidth
            options.inSampleSize = max(wr, hr)
        }
        options.inJustDecodeBounds = false
        var source = BitmapFactory.decodeFile(sourcePath, options)

        if (rotation != 0) {
            // 旋转Bitmap
            width = source.width
            height = source.height
            val matrix = Matrix()
            matrix.setRotate(rotation.toFloat(), width / 2f, height / 2f)
            val temp = source
            source = Bitmap.createBitmap(source, 0, 0, width, height, matrix, false)
            temp.recycle()
        }
        return source
    }

    private fun initEvent() {
        iv_switch.setOnClickListener {
            toggleCamera()
        }

        capture_layout.setTypeLisenter(object : TypeListener {
            override fun confirm() {
                setResult()
            }

            override fun cancel() {
                fotoapparat.start()
                iv_switch.visibility = View.VISIBLE
                photo_preview.visibility = View.GONE
                video_preview.visibility = View.GONE
                video_preview.stopPlayback()

                deleteTempFile()// 删除临时文件
                Log.i("CameraDefaultActivity", "cancel: ")
            }
        })
        capture_layout.setLeftClickListener {
            finish()
        }
        capture_layout.setCaptureLisenter(object : CaptureListener {
            override fun recordEnd(time: Long) {
                videoTaker.stopRecording()
            }

            override fun recordZoom(zoom: Float) {
            }

            override fun takePictures() {
                captureFile = if (TextUtils.isEmpty(capturePath)) {
                    PathGetter.generaCapturePath(this@CameraDefaultActivity)
                } else {
                    File(capturePath)
                }
                fotoapparat.takePicture()
                    .saveToFile(captureFile)
                    .whenAvailable(this@CameraDefaultActivity::onTakePic)
            }

            override fun recordShort(time: Long) {
                videoTaker.stopRecording(true)
            }

            override fun recordStart() {
                videoTaker.startRecording()
            }
        })
    }

    private fun setResult() {
        val intent = Intent()
        if (::captureFile.isInitialized) {
            intent.putExtra(EXTRA_CAPTURE_OUTPUT_PATH, captureFile.absoluteFile)
        }
        if (::recorderFile.isInitialized) {
            intent.putExtra(EXTRA_RECORDER_OUTPUT_PATH, recorderFile.absoluteFile)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        fotoapparat.start()
    }

    override fun onStop() {
        super.onStop()
        fotoapparat.stop()
    }

    private fun toggleCamera() {
        if (isFront) {
            fotoapparat.switchTo(
                lensPosition = back(),
                cameraConfiguration = CameraConfiguration.default()
            )
        } else {
            fotoapparat.switchTo(
                lensPosition = front(),
                cameraConfiguration = CameraConfiguration.default()
            )
        }
        isFront = !isFront
    }

    private fun deleteTempFile() {
        GlobalScope.launch(Dispatchers.IO) {
            if (!TextUtils.isEmpty(capturePath) && ::captureFile.isInitialized) {
                captureFile.delete()
            }
            if (!TextUtils.isEmpty(recorderPath) && ::recorderFile.isInitialized) {
                recorderFile.delete()
            }
        }
    }

    override fun onDestroy() {
        video_preview.stopPlayback()
        super.onDestroy()
    }
}