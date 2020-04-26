package com.dede.fotoapparat_extend

import com.dede.fotoapparat_extend.recorder.VideoConfig
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.firstAvailable
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.selector.standardRatio
import io.fotoapparat.selector.wideRatio


/**
 * 指定比例的相机配置
 *
 * 分辨率优先级:
 *  16/9 最高像素
 *  4/3 最高像素
 *  其他比例最高像素
 */
fun ratioCameraConfiguration(): CameraConfiguration {
    val ratioResolution = firstAvailable(
        wideRatio(highestResolution()),// 16/9
        standardRatio(highestResolution()),// 4/3
        highestResolution()
    )
    return CameraConfiguration.default().copy(
        previewResolution = ratioResolution,
        pictureResolution = ratioResolution
    )
}

/**
 * 采集视频配置
 *
 * fotoapparat.takeVideo()
 *      .saveToFile(recorderFile)
 *      .maxDuration(maxDuration)
 *      .takeVideo { video ->
 *          // video obj
 *      }
 */
fun Fotoapparat.takeVideo(): VideoConfig {
    return VideoConfig.from(this)
}