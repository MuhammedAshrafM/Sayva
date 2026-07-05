package org.moashraf.sayva.pipeline

import org.moashraf.sayva.camera.CameraFrame

/**
 * On iOS the platform frame is a `CVPixelBuffer` handed to us by
 * AVCaptureVideoDataOutput. ARC handles its release; no explicit close.
 * When the real iOS camera lands (P2-S5) this can stay a no-op.
 */
internal actual fun closePlatformFrame(frame: CameraFrame) {
    // no-op
}
