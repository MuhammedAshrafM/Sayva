package org.moashraf.sayva.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * iOS [CameraPreview] placeholder. Renders a black surface with a
 * "iOS camera preview pending Mac build" message so developers running
 * an iOS build without the real implementation see something explicit.
 * Replace during P2-S5 with a `UIKitView` embedding an
 * `AVCaptureVideoPreviewLayer`.
 */
@Composable
actual fun CameraPreview(controller: CameraController, modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Camera preview lands in P2-S5",
            color = Color.White,
        )
    }
}
