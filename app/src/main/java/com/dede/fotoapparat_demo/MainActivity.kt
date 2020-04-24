package com.dede.fotoapparat_demo

import android.Manifest
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.dede.default_camera.CameraDefaultActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 0
        )
    }

    fun takePic(view: View) {
        val intent = Intent(this, CameraDefaultActivity::class.java)
            .putExtra(
                CameraDefaultActivity.EXTRA_BUTTON_TYPE,
                CameraDefaultActivity.BUTTON_STATUS_ONLY_CAPTURE
            )
        startActivityForResult(intent, 0)
    }


    fun takeVideo(view: View) {
        val intent = Intent(this, CameraDefaultActivity::class.java)
            .putExtra(
                CameraDefaultActivity.EXTRA_BUTTON_TYPE,
                CameraDefaultActivity.BUTTON_STATUS_ONLY_RECORDER
            )
        startActivityForResult(intent, 0)
    }

    fun takeAll(view: View) {
        val intent = Intent(this, CameraDefaultActivity::class.java)
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        video_preview.visibility = View.GONE
        photo_preview.visibility = View.GONE

        val video = data?.getStringExtra(CameraDefaultActivity.EXTRA_RECORDER_OUTPUT_PATH)
        if (!TextUtils.isEmpty(video)) {
            Log.i("MainActivity", "onActivityResult: " + video)
            video_preview.visibility = View.VISIBLE
            video_preview.setOnPreparedListener {
                video_preview.start()
            }
            video_preview.setVideoURI(Uri.fromFile(File(video)))
        }
        val pic = data?.getStringExtra(CameraDefaultActivity.EXTRA_CAPTURE_OUTPUT_PATH)
        if (!TextUtils.isEmpty(pic)) {
            Log.i("MainActivity", "onActivityResult: " + pic)
            photo_preview.visibility = View.VISIBLE
            photo_preview.setImageBitmap(transformationBitmap(File(pic)))
        }
    }

    private fun transformationBitmap(sourceFile: File): Bitmap? {
        if (!sourceFile.exists()) {
            return null
        }
        val sourcePath = sourceFile.absolutePath

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

    override fun onDestroy() {
        video_preview.stopPlayback()
        super.onDestroy()
    }

}
