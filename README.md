# Fotoapparat Extend

一个基于[Fotoapparat](https://github.com/RedApparat/Fotoapparat)和MediaRecorder的相机扩展。使用了反射获取Fotoapparat内Camera实例进行的视频录制。

扩展的功能：
- 添加了视频录制功能
- 提供了一个默认宽高比的相机配置(16/9，4/3)
- 提供了一个默认的相机页(仿照微信)，支持拍照和录制视频。


录制视频：
```kotlin
val fotoapparat = Fotoapparat(
    context = this,
    view = cameraView,
    cameraConfiguration = ratioCameraConfiguration()// 保持16/9或4/3宽高比的配置
)

fotoapparat.start()

val videoTaker = fotoapparat.takeVideo()
    .saveToFile(recorderFile)
    .takeVideo { video ->
        // 录制结果
    }
videoTaker.startRecording()

videoTaker.stopRecording()
```

默认录制视频为720p 8500000pbs 30FPS的mp4(H264 AAC)格式

使用默认相机页：
```kotlin
val intent = Intent(this, CameraDefaultActivity::class.java)
startActivityForResult(intent, 0)

// onActivityResult
val video = data.getStringExtra(CameraDefaultActivity.EXTRA_RECORDER_OUTPUT_PATH)
val pic = data.getStringExtra(CameraDefaultActivity.EXTRA_CAPTURE_OUTPUT_PATH)
```

其他API，详见[Fotoapparat Wiki](https://github.com/RedApparat/Fotoapparat/blob/master/README.md)

---

混淆配置：
```
-keep class io.fotoapparat.hardware.CameraDevice {*;}
-keep class io.fotoapparat.hardware.Device {*;}
-keep class io.fotoapparat.characteristic.Characteristics {*;}
```