package com.dede.fotoapparat_extend.recorder

import io.fotoapparat.parameter.Resolution
import java.io.File

/**
 * Created by hsh on 2020/4/23 6:05 PM
 */
data class Video(
    val sourceFile: File,
    val rotationDegrees: Int,
    val videoResolution: Resolution
)

typealias VideoResult = (Video?) -> Unit