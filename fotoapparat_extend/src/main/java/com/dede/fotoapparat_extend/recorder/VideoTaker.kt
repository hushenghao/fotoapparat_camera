package com.dede.fotoapparat_extend.recorder

import android.hardware.Camera
import android.media.CamcorderProfile
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import com.dede.fotoapparat_extend.exception.VideoTakeException
import com.dede.fotoapparat_extend.reflect.fieldValue
import com.dede.fotoapparat_extend.reflect.safeFieldValue
import io.fotoapparat.Fotoapparat
import io.fotoapparat.error.CameraErrorCallback
import io.fotoapparat.exception.camera.CameraException
import io.fotoapparat.hardware.orientation.Orientation
import io.fotoapparat.log.Logger
import io.fotoapparat.parameter.Resolution
import io.fotoapparat.view.CameraRenderer
import io.fotoapparat.view.Preview
import kotlinx.coroutines.*

/**
 * Created by hsh on 2020/4/24 9:21 AM
 */
class VideoTaker internal constructor(
    private val fotoapparat: Fotoapparat,
    private val config: VideoConfig,
    private val videoResult: VideoResult
) : MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    companion object {
        private val errorHandler = CoroutineExceptionHandler { _, e ->
            e.printStackTrace()
        }

        val baseScope: CoroutineScope = MainScope() + errorHandler
    }

    private var cameraRenderer: CameraRenderer? = null
    private var logger: Logger? = null
    private var mainThreadErrorCallback: CameraErrorCallback? = null

    // io.fotoapparat.hardware.Device
    private val device: Any

    private lateinit var mediaRecorder: MediaRecorder

    init {
        try {
            // io.fotoapparat.hardware.Device
            device = fotoapparat.fieldValue("device")
        } catch (e: Exception) {
            throw VideoTakeException("get Device instance error", e)
        }

        cameraRenderer = device.safeFieldValue("cameraRenderer")
        logger = fotoapparat.safeFieldValue("logger")
        mainThreadErrorCallback = fotoapparat.safeFieldValue("mainThreadErrorCallback")
    }

    private fun getCamera(cameraDevice: Any): Camera? {
        return cameraDevice.safeFieldValue<Camera>("camera")
    }

    private fun getCameraCharacteristics(cameraDevice: Any): Any? {
        // io.fotoapparat.characteristic.Characteristics
        return cameraDevice.safeFieldValue("characteristics")
    }

    private fun getCameraId(characteristics: Any?): Int {
        return characteristics?.safeFieldValue<Int>("cameraId") ?: 0
    }

    private fun getCameraOrientation(characteristics: Any?): Int {
        return characteristics?.safeFieldValue<Orientation>("cameraOrientation")?.degrees
            ?: 0
    }

    private fun transformResolution(cameraOrientation: Int, profile: CamcorderProfile): Resolution {
        return when (cameraOrientation) {
            90, 270 -> {
                Resolution(profile.videoFrameHeight, profile.videoFrameWidth)
            }
            else -> {
                Resolution(profile.videoFrameWidth, profile.videoFrameHeight)
            }
        }
    }

    private suspend fun initRecorder(): MediaRecorder {
        // CompletableDeferred<io.fotoapparat.hardware.CameraDevice>
        val selectedCameraDevice = device.fieldValue<CompletableDeferred<*>>("selectedCameraDevice")
        val cameraDevice = selectedCameraDevice.await()
            ?: throw IllegalStateException("await CameraDevice error")

        mediaRecorder = MediaRecorder()
        val camera = getCamera(cameraDevice)
        val cameraCharacteristics = getCameraCharacteristics(cameraDevice)
        val cameraId = getCameraId(cameraCharacteristics)
        val cameraOrientation = getCameraOrientation(cameraCharacteristics)
        val camcorderProfile: CamcorderProfile = getCamcorderProfile(cameraId)

        val videoResolution =
            Resolution(camcorderProfile.videoFrameWidth, camcorderProfile.videoFrameHeight)
        video = Video(config.file, cameraOrientation, videoResolution)

        logger?.log("cameraId: $cameraId")
        logger?.log("cameraOrientation: $cameraOrientation")
        logger?.log("videoFrameWidth: " + camcorderProfile.videoFrameWidth)
        logger?.log("videoFrameHeight: " + camcorderProfile.videoFrameHeight)
        logger?.log("normal videoBitRate: " + camcorderProfile.videoBitRate)
        logger?.log("normal videoFrameRate: " + camcorderProfile.videoFrameRate)

//        val parameters = camera?.parameters
//        if (parameters != null) {
        val previewSize = fotoapparat.getCurrentParameters().await().previewResolution
//
//            val vw = camcorderProfile.videoFrameWidth
//            val vh = camcorderProfile.videoFrameHeight
//            val vr = vw * 1f / vh
//
        val pw = previewSize.width
        val ph = previewSize.height
//            val pr = pw * 1f / ph
        logger?.log("previewSize:  $pw : $ph")
//
//            if (pr != vr) {
//                logger?.log("videoSize:  $vw : $vh")
//                for (size in parameters.supportedPreviewSizes) {
//                    Log.i("VideoTaker", "supportPreviewSize (${size.width}:${size.height})")
//                }
        // 宽高比不相同，需要修改预览宽高为视频宽高
//                parameters.setPreviewSize(vw, vh)
//                try {
//                    camera.parameters = parameters
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//                val resolution = transformResolution(cameraOrientation, camcorderProfile)
//                cameraRenderer?.setPreviewResolution(resolution)
//            }
//        }

        camera?.unlock()// 解锁相机
        mediaRecorder.setCamera(camera)

        val preview = cameraRenderer?.getPreview()
        if (preview is Preview.Texture) {
            mediaRecorder.setPreviewDisplay(Surface(preview.surfaceTexture))
        } else if (preview is Preview.Surface) {
            mediaRecorder.setPreviewDisplay(preview.surfaceHolder.surface)
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        if (config.maxDuration > 0) {
            mediaRecorder.setMaxDuration(config.maxDuration)
        }
        if (config.maxFileSize > 0) {
            mediaRecorder.setMaxFileSize(config.maxFileSize)
        }
        camcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4
        camcorderProfile.audioCodec = MediaRecorder.AudioEncoder.AAC
        camcorderProfile.videoCodec = MediaRecorder.VideoEncoder.H264
        camcorderProfile.videoFrameRate = config.videoFrameRate
        camcorderProfile.videoBitRate = config.videoBitRate
        mediaRecorder.setProfile(camcorderProfile)
        mediaRecorder.setOrientationHint(cameraOrientation)

        val output = config.file
        if (output.isDirectory) {
            throw IllegalArgumentException("output file is dir")
        }
        if (output.exists()) {
            logger?.log("output file already exists, deleted")
            output.delete()
        }
        output.createNewFile()
        mediaRecorder.setOutputFile(output.absolutePath)
        mediaRecorder.setOnErrorListener(this)
        mediaRecorder.setOnInfoListener(this)

        return mediaRecorder
    }

    override fun onError(mr: MediaRecorder?, what: Int, extra: Int) {
        logger?.log("MediaRecorder Error   what: ${what}, extra: ${extra}")
    }

    override fun onInfo(mr: MediaRecorder?, what: Int, extra: Int) {
        when (what) {
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED -> {
                stopRecording()
            }
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> {
                stopRecording()
            }
        }
    }

    private fun getCamcorderProfile(cameraId: Int): CamcorderProfile {
        val camcorderProfile: CamcorderProfile
        if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P)) {
            camcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P)
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P)) {
            camcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P)
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P)) {
            camcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P)
        } else if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH)) {
            camcorderProfile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH)
        } else {
            throw IllegalStateException("CamcorderProfile un support")
        }
        return camcorderProfile
    }

    private var isRecording = false
    private var job: Job? = null
    private var video: Video? = null

    fun isRecording(): Boolean {
        return isRecording
    }

    fun startRecording() {
        logger?.recordMethod()
        job = (baseScope + CoroutineExceptionHandler { _, e ->
            mainThreadErrorCallback?.invoke(CameraException("startRecording error", e))
        }).launch {
            Log.i("VideoTaker", "startRecording: " + isActive)
            val mediaRecorder = withContext(Dispatchers.IO) {
                initRecorder().also { it.prepare() }
            }
            if (!isActive) return@launch
            mediaRecorder.start()
            isRecording = true
        }
    }

    fun stopRecording() {
        stopRecording(false)
    }

    fun stopRecording(ignore: Boolean) {
        logger?.recordMethod()
        if (!::mediaRecorder.isInitialized || !isRecording) {
            return
        }
        job?.cancel()
        try {
            mediaRecorder.stop()
            mediaRecorder.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isRecording = false
        if (!ignore) {
            videoResult.invoke(video)
        }
    }

}