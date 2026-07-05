package org.moashraf.sayva.nav

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/**
 * Android actual — [androidx.activity.compose.BackHandler] handles both the
 * classic Back button and the predictive-back gesture on API 33+. When
 * [enabled] is `false`, our handler yields to the platform default, which
 * finishes the Activity (typical "exit app from root screen" behavior).
 */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
