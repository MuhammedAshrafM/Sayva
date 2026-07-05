package org.moashraf.sayva.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Renders the camera preview surface. Platform-actual composables use:
 *   * Android — `AndroidView` embedding CameraX's `PreviewView`
 *   * iOS — `UIKitView` embedding an `AVCaptureVideoPreviewLayer`-backed view
 *
 * The controller must be [CameraController.start]ed before this composable
 * shows something meaningful. Callers usually wrap this in a `Box` with a
 * landmark overlay drawn on top.
 */
@Composable
expect fun CameraPreview(
    controller: CameraController,
    modifier: Modifier = Modifier,
)
