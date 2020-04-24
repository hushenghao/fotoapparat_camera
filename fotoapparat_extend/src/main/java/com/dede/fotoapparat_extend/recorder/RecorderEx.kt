package com.dede.fotoapparat_extend.recorder

import io.fotoapparat.Fotoapparat

/**
 * Created by hsh on 2020/4/23 6:03 PM
 */

fun Fotoapparat.takeVideo(): VideoConfig {
    return VideoConfig.from(this)
}