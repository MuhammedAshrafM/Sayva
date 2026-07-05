package org.moashraf.sayva.camera

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Android [CameraPreview] — embeds CameraX's `PreviewView` in Compose via
 * [AndroidView]. The view's surface provider is wired up by
 * [CameraControllerImpl.start].
 *
 * The `PreviewView` is owned by the controller (lifetime-scoped to the
 * controller instance), so recreating this composable doesn't kill the
 * camera bindings.
 */
@Composable
actual fun CameraPreview(
    controller: CameraController,
    modifier: Modifier,
) {
    val impl = controller as? CameraControllerImpl
        ?: error(
            "CameraPreview only knows how to render CameraControllerImpl instances. " +
                "Got ${controller::class.simpleName}."
        )
    AndroidView(
        modifier = modifier,
        factory = { impl.previewView },
    )
}
