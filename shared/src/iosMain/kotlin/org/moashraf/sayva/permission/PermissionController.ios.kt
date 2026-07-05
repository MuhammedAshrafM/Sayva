package org.moashraf.sayva.permission

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation. Camera and microphone checks are synchronous per Apple's SDK;
 * notification check is bridged to suspend via [suspendCancellableCoroutine].
 *
 * "Granted" for notifications includes `.provisional` and `.ephemeral` statuses —
 * both allow the app to deliver notifications, they just differ in UX (provisional
 * = quiet delivery; ephemeral = App Clip-only). If your UX requires user-visible
 * banners specifically, tighten this to `.authorized` only.
 */
class IosPermissionController : PermissionController {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun isGranted(permission: SayvaPermission): Boolean =
        when (permission) {
            SayvaPermission.Camera ->
                AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
                    AVAuthorizationStatusAuthorized

            SayvaPermission.Microphone ->
                AVAudioSession.sharedInstance().recordPermission ==
                    AVAudioSessionRecordPermissionGranted

            SayvaPermission.Notifications -> checkNotificationsGranted()
        }

    @OptIn(ExperimentalForeignApi::class)
    override fun openAppSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun checkNotificationsGranted(): Boolean =
        suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler {
                settings ->
                val status = settings?.authorizationStatus
                val granted = status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional ||
                    status == UNAuthorizationStatusEphemeral
                if (cont.isActive) cont.resume(granted)
            }
        }
}

actual object PermissionControllerProvider {
    actual fun create(): PermissionController = IosPermissionController()
}
