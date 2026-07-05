package org.moashraf.sayva.camera

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * iOS [CameraController] stub — real implementation deferred to the Mac
 * session (P2-S5). Fills in with `AVCaptureSession` + `AVCaptureVideoDataOutput`,
 * hosted inside a Compose `UIKitView`.
 *
 * The stub throws on [start] so a Phase 2 screen mistakenly running against
 * it produces a clear error rather than a silent no-op.
 */
internal class CameraControllerStub : CameraController {

    private val _frames = MutableSharedFlow<CameraFrame>()
    override val frames: SharedFlow<CameraFrame> = _frames.asSharedFlow()
    override val lens: CameraLens = CameraLens.Front

    override suspend fun start() {
        throw NotImplementedError(
            "iOS CameraController not implemented in this build. Fill in during Mac " +
                "session — AVCaptureSession + AVCaptureVideoDataOutput bridged into " +
                "the shared module via UIKitView."
        )
    }

    override suspend fun stop() {}
    override suspend fun switchLens(lens: CameraLens) {}
}

actual object CameraControllerProvider {
    actual fun create(): CameraController = CameraControllerStub()
}
