package org.moashraf.sayva.camera

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS [CameraController] stub — real implementation deferred to the Mac
 * session (P2-S5). Fills in with `AVCaptureSession` + `AVCaptureVideoDataOutput`,
 * hosted inside a Compose `UIKitView`. Torch will be wired via
 * `AVCaptureDevice.torchMode` at that time; the interface points are
 * declared here so commonMain callers don't need to know it's stubbed.
 *
 * The stub throws on [start] so a Phase 2 screen mistakenly running against
 * it produces a clear error rather than a silent no-op.
 */
internal class CameraControllerStub : CameraController {

    private val _frames = MutableSharedFlow<CameraFrame>()
    override val frames: SharedFlow<CameraFrame> = _frames.asSharedFlow()
    override val lens: CameraLens = CameraLens.Front

    override val hasTorch: Boolean = false
    private val _torchEnabled = MutableStateFlow(false)
    override val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    override suspend fun start() {
        throw NotImplementedError(
            "iOS CameraController not implemented in this build. Fill in during Mac " +
                "session — AVCaptureSession + AVCaptureVideoDataOutput bridged into " +
                "the shared module via UIKitView."
        )
    }

    override suspend fun stop() {}
    override suspend fun switchLens(lens: CameraLens) {}
    override suspend fun setTorchEnabled(enabled: Boolean) {
        // No-op until AVCaptureDevice is wired; call is harmless because
        // hasTorch is `false`, so UI won't reach this in the stub build.
    }
}

actual object CameraControllerProvider {
    actual fun create(): CameraController = CameraControllerStub()
}
