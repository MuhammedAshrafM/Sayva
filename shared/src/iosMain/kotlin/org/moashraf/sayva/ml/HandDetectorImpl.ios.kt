package org.moashraf.sayva.ml

import org.moashraf.sayva.camera.CameraFrame

/**
 * iOS [HandDetector] stub — real implementation deferred to Mac session.
 * MediaPipe iOS is added via CocoaPods (`MediaPipeTasksVision`) with a
 * cinterop def wrapping `MPPHandLandmarker`.
 */
internal class HandDetectorStub : HandDetector {
    override fun detect(frame: CameraFrame): HandDetection {
        throw NotImplementedError(
            "iOS HandDetector not implemented. Add MediaPipe iOS pod + cinterop " +
                "in the Mac session and construct MPPHandLandmarker here."
        )
    }
    override fun close() {}
}

actual object HandDetectorProvider {
    actual fun create(maxHands: Int): HandDetector = HandDetectorStub()
}
