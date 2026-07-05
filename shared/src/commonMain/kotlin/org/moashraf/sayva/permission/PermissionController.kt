package org.moashraf.sayva.permission

/**
 * Platform-agnostic permission state accessor.
 *
 * Scope: **state checks and settings deep-link only**. Actual runtime permission
 * *requests* (showing the system dialog) are done at call-site via Compose's
 * `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())`
 * on Android and direct SDK calls on iOS — those need Composable/UI scope which
 * a Koin-scoped controller doesn't have.
 *
 * ### Why this split
 * The permission model has two orthogonal concerns:
 *   1. "Am I currently granted permission X?" — query, no UI. Belongs here.
 *   2. "Show the system dialog to request X." — needs UI/Activity scope. Belongs
 *      in composables using Activity Result API.
 *
 * Repositories and gateway adapters use (1) to decide whether to proceed. UI
 * flows (PermissionsScreen, LiveCameraScreen's guard) use (2). Splitting them
 * keeps the abstraction clean.
 */
interface PermissionController {

    /**
     * Whether [permission] is currently granted.
     *
     * Suspending because iOS's UNUserNotificationCenter check is async — camera
     * and mic checks on both platforms are synchronous, but we normalize on
     * suspending for a uniform call site.
     */
    suspend fun isGranted(permission: SayvaPermission): Boolean

    /**
     * Opens the OS settings page for this app, where the user can grant
     * permissions manually. Use as a fallback when a permission was denied
     * permanently (Android "don't ask again" or iOS second-time-denied).
     */
    fun openAppSettings()
}

/**
 * Permissions the app requests. Kept as a small closed enum rather than a raw
 * string map so we can't accidentally spell them wrong and so the compiler tells
 * us if a permission is dropped without a migration path.
 */
enum class SayvaPermission {
    /** Camera access — required for the core translation feature. */
    Camera,

    /** Microphone access — required for two-way conversation voice input. */
    Microphone,

    /** Post-notifications permission (Android 13+; always granted on lower versions and iOS). */
    Notifications,
}

/** Platform factory, resolved by Koin as `single<PermissionController>`. */
expect object PermissionControllerProvider {
    fun create(): PermissionController
}
