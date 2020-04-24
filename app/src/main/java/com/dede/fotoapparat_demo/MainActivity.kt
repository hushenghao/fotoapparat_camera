package com.dede.fotoapparat_demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dede.default_camera.CameraDefaultActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
        val video = data?.getStringExtra(CameraDefaultActivity.EXTRA_RECORDER_OUTPUT_PATH)
        Log.i("MainActivity", "onActivityResult: " + video)
        val pic = data?.getStringExtra(CameraDefaultActivity.EXTRA_CAPTURE_OUTPUT_PATH)
        Log.i("MainActivity", "onActivityResult: " + pic)
    }

}
