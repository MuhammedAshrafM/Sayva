package org.moashraf.sayva.pipeline

import androidx.camera.core.ImageProxy
import org.moashraf.sayva.camera.CameraFrame

/**
 * CameraX hands us an [ImageProxy] per frame; the analysis pipeline MUST
 * close it after processing or CameraX stalls waiting for frame buffers
 * to free up. This is the one platform-specific concern the recognition
 * pipeline touches.
 */
internal actual fun closePlatformFrame(frame: CameraFrame) {
    (frame.platformFrame as? ImageProxy)?.close()
}
