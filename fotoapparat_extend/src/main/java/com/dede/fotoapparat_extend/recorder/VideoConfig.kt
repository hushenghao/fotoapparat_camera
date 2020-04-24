package com.dede.fotoapparat_extend.recorder

import androidx.annotation.IntRange
import io.fotoapparat.Fotoapparat
import java.io.File

/**
 * Created by hsh on 2020/4/24 9:22 AM
 */
class VideoConfig internal constructor(private val fotoapparat: Fotoapparat) {

    companion object {
        fun from(fotoapparat: Fotoapparat): VideoConfig {
            return VideoConfig(fotoapparat)
        }
    }

    internal var maxDuration: Int = -1
    internal var maxFileSize: Long = -1
    internal var videoFrameRate: Int = 30
    internal var videoBitRate: Int = 8500_000
    internal lateinit var file: File

    fun videoFrameRate(@IntRange(from = 0) videoFrameRate: Int): VideoConfig {
        this.videoFrameRate = videoFrameRate
        return this
    }

    fun videoBitRate(@IntRange(from = 0) videoBitRate: Int): VideoConfig {
        this.videoBitRate = videoBitRate
        return this
    }

    fun maxDuration(@IntRange(from = 0) maxDuration: Int): VideoConfig {
        this.maxDuration = maxDuration
        return this
    }

    fun maxFileSize(@IntRange(from = 0) maxFileSize: Long): VideoConfig {
        this.maxFileSize = maxFileSize
        return this
    }

    fun saveToFile(file: File): VideoConfig {
        this.file = file
        return this
    }

    fun takeVideo(videoResult: VideoResult): VideoTaker {
        if (!::file.isInitialized) {
            throw IllegalArgumentException("saveToFile file == null")
        }
        return VideoTaker(fotoapparat, this, videoResult)
    }

}