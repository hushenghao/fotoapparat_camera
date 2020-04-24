package com.dede.default_camera

import android.content.Context
import java.io.File
import java.util.*

/**
 * Created by hsh on 2020/4/24 5:38 PM
 */
object PathGetter {
    fun generaRecorderPath(context: Context): File {
        var filesDir = context.getExternalFilesDir("videos")
        if (filesDir == null) {
            filesDir = File(context.filesDir, "videos")
        }
        return File(filesDir, generaFileName(".mp4"))
    }

    fun generaCapturePath(context: Context): File {
        var filesDir = context.getExternalFilesDir("images")
        if (filesDir == null) {
            filesDir = File(context.filesDir, "images")
        }
        return File(filesDir, generaFileName(".jpg"))
    }

    private fun generaFileName(suffix: String): String {
        return UUID.randomUUID().toString() + suffix
    }
}