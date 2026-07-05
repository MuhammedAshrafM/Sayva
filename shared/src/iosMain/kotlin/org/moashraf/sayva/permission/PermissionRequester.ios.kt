package org.moashraf.sayva.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * iOS implementation. Each of the three permission types uses a distinct SDK:
 *
 *   - Camera        → `AVCaptureDevice.requestAccessForMediaType`
 *   - Microphone    → `AVAudioSession.sharedInstance().requestRecordPermission`
 *   - Notifications → `UNUserNotificationCenter.requestAuthorizationWithOptions`
 *
 * All three callbacks fire on background queues on some device configurations,
 * so we marshal back to the main queue before invoking [onResult] — the
 * Compose state that the caller updates must be touched on the main thread.
 *
 * ### Camera pre-check
 * iOS silently no-ops `requestAccessForMediaType` after the first denial —
 * subsequent calls invoke the completion with `false` without showing a dialog.
 * That's fine, but callers should detect the "denied" state via
 * [PermissionController.openAppSettings] to guide the user manually.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberPermissionRequester(
    onResult: (SayvaPermission, Boolean) -> Unit,
): PermissionRequester {
    val currentOnResult by rememberUpdatedState(onResult)

    // Marshaling to main queue — Compose state on iOS is main-thread only.
    fun deliver(permission: SayvaPermission, granted: Boolean) {
        dispatch_async(dispatch_get_main_queue()) {
            currentOnResult(permission, granted)
        }
    }

    return remember {
        PermissionRequester { permission ->
            when (permission) {
                SayvaPermission.Camera -> {
                    // Fast-path if already authorized — SDK still fires the callback
                    // but there's no dialog delay.
                    val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                    if (status == AVAuthorizationStatusAuthorized) {
                        deliver(permission, true)
                    } else {
                        AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                            deliver(permission, granted)
                        }
                    }
                }
                SayvaPermission.Microphone -> {
                    AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                        deliver(permission, granted)
                    }
                }
                SayvaPermission.Notifications -> {
                    val options = UNAuthorizationOptionAlert or
                        UNAuthorizationOptionBadge or
                        UNAuthorizationOptionSound
                    UNUserNotificationCenter.currentNotificationCenter()
                        .requestAuthorizationWithOptions(options) { granted, _ ->
                            deliver(permission, granted)
                        }
                }
            }
        }
    }
}
