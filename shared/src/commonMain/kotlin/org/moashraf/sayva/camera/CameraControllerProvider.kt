package org.moashraf.sayva.camera

/**
 * Platform factory — instantiates a [CameraController] with per-platform
 * bindings (Android needs the Activity; iOS needs the current AVCaptureSession
 * dispatcher). Bound via Koin as a `single`.
 *
 * Depends on:
 *   * Android — [org.moashraf.sayva.data.AndroidAppContext] + AndroidActivityProvider
 *     (already established for BiometricPrompt, CrashReporter, etc.)
 *   * iOS — nothing at construction time; queues bindings until [start]
 */
expect object CameraControllerProvider {
    fun create(): CameraController
}
