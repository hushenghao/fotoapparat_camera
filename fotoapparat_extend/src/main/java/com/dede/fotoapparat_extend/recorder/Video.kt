package com.dede.fotoapparat_extend.recorder

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.provider.MediaStore
import io.fotoapparat.parameter.Resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Created by hsh on 2020/4/23 6:05 PM
 */
data class Video internal constructor(
    val sourceFile: File,
    val rotationDegrees: Int,
    val videoResolution: Resolution
) {
    var duration: Long = -1
        internal set

    suspend fun getThumbnail(): Bitmap? {
        return thumbnail()
    }
}

internal suspend fun Video.thumbnail() = withContext(Dispatchers.IO) {
    ThumbnailUtils.createVideoThumbnail(
        sourceFile.absolutePath,
        MediaStore.Images.Thumbnails.MINI_KIND
    )
}

internal suspend fun Video.perfect() = withContext(Dispatchers.IO) {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(sourceFile.absolutePath)
        val durationStr =
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        var duration = -1L
        try {
            duration = durationStr.toLong()
        } catch (e: NumberFormatException) {
        }
        this@perfect.duration = duration
    } catch (e: IllegalArgumentException) {
    } catch (e: RuntimeException) {
    } finally {
        try {
            retriever.release()
        } catch (e: RuntimeException) {
        }
    }
}

typealias VideoResult = (Video?) -> Unit