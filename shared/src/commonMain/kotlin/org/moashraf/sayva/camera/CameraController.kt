package org.moashraf.sayva.camera

import kotlinx.coroutines.flow.SharedFlow

/**
 * Cross-platform camera abstraction — the port through which the recognition
 * pipeline receives frames.
 *
 * ### Design contract
 * A single [CameraController] owns:
 *   * The device camera (front-facing by default — signers face the camera)
 *   * A preview surface hosted by a Compose `@Composable Preview()` function
 *     (see `CameraPreview.kt`, platform-actual)
 *   * A frame flow — one [CameraFrame] per captured frame, dropped if the
 *     downstream consumer can't keep up
 *
 * Lifecycle is scope-driven: [start] binds the camera to the caller's
 * `CoroutineScope`; when the scope cancels, [stop] runs implicitly.
 * Callers should launch [start] inside a `LaunchedEffect` tied to the
 * camera screen — cancelling the effect stops the camera cleanly.
 */
interface CameraController {

    /** Cold flow — starts capturing on first `collect`, stops when cancelled. */
    val frames: SharedFlow<CameraFrame>

    /**
     * Bind the camera to the platform's lifecycle (Android: ProcessCameraProvider,
     * iOS: AVCaptureSession). No-op if already started.
     */
    suspend fun start()

    /** Unbind and release native resources. Idempotent. */
    suspend fun stop()

    /** Toggle between front + back camera. Recognition uses front-facing by default. */
    suspend fun switchLens(lens: CameraLens)

    /** Which camera is currently active. */
    val lens: CameraLens
}

enum class CameraLens { Front, Back }

/**
 * One camera frame — pixel data ready for downstream detection.
 *
 * On Android this wraps a CameraX `ImageProxy` (accessed via [asPlatformFrame]);
 * on iOS a `CVPixelBuffer`. The recognition pipeline is platform-agnostic and
 * consumes the derived landmarks from [HandDetector], not raw pixels — this
 * class is a passthrough token.
 *
 * @param widthPx captured frame width in pixels
 * @param heightPx captured frame height in pixels
 * @param rotationDegrees rotation applied by the platform (0/90/180/270)
 * @param timestampNanos monotonic timestamp — used for FPS calculation
 * @param platformFrame opaque per-platform frame handle (ImageProxy on
 *   Android, CVPixelBuffer on iOS). Consumers cast via `expect fun
 *   asPlatformFrame(...)` accessors in platform-specific bridges.
 */
data class CameraFrame(
    val widthPx: Int,
    val heightPx: Int,
    val rotationDegrees: Int,
    val timestampNanos: Long,
    val platformFrame: Any,
)
