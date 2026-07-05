package org.moashraf.sayva.nav

import androidx.compose.runtime.Composable

/**
 * Intercept the platform's system-back gesture (Android system Back button
 * or predictive back; iOS has no equivalent so the actual is a no-op).
 *
 * Wire once at the top of the nav tree — when `enabled` is `true`, the
 * system back is routed to [onBack]; when `false`, it falls through to the
 * default behavior (Activity finish → app exits).
 *
 * ### Usage
 * ```
 * SystemBackHandler(enabled = nav.canGoBack) { nav.back() }
 * ```
 *
 * Rendered with `enabled = false` when the user is on the root screen so
 * back still exits the app naturally — matching Android convention.
 */
@Composable
expect fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit)
