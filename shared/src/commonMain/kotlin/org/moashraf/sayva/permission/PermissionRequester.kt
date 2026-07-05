package org.moashraf.sayva.permission

import androidx.compose.runtime.Composable

/**
 * UI-scoped handle for triggering system permission dialogs.
 *
 * ### Why this is separate from [PermissionController]
 * `PermissionController` handles read-only concerns (isGranted / openAppSettings)
 * and lives in Koin. Requesting a permission needs UI scope — Android's
 * `ActivityResultContracts.RequestPermission()` launcher must be registered
 * inside a Composable, and iOS's AVFoundation / UNUserNotificationCenter
 * callbacks fire on background queues that need marshaling. Bundling that into
 * a Koin-scoped controller would tangle lifecycle with DI.
 *
 * ### Usage
 * ```
 * val requester = rememberPermissionRequester { permission, granted ->
 *     grants[permission] = granted
 * }
 * PrimaryButton(onClick = { requester.request(SayvaPermission.Camera) })
 * ```
 *
 * The result is delivered asynchronously via the `onResult` callback wired
 * inside [rememberPermissionRequester] — `request()` itself returns immediately.
 */
class PermissionRequester internal constructor(
    private val requestImpl: (SayvaPermission) -> Unit,
) {
    /**
     * Show the system permission dialog for [permission]. On platforms where
     * the permission is grant-by-manifest (e.g. Android POST_NOTIFICATIONS on
     * API < 33), the implementation may immediately fire the `onResult`
     * callback with `granted = true` without a dialog.
     */
    fun request(permission: SayvaPermission) = requestImpl(permission)
}

/**
 * Build a [PermissionRequester] wired to the current UI scope. Result is
 * delivered via [onResult] on the main thread on both platforms.
 */
@Composable
expect fun rememberPermissionRequester(
    onResult: (SayvaPermission, Boolean) -> Unit,
): PermissionRequester
