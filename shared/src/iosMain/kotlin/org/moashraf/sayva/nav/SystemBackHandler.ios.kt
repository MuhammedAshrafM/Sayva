package org.moashraf.sayva.nav

import androidx.compose.runtime.Composable

/**
 * iOS actual — iOS has no system-back gesture (users swipe back inside
 * navigation controllers, which is the in-app back button we render).
 * This is intentionally a no-op so the same call site works for both
 * platforms; on iOS the app's own back chevron is the source of truth.
 */
@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}
