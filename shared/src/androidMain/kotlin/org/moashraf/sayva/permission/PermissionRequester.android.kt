package org.moashraf.sayva.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Android implementation. Registers three [ActivityResultContracts.RequestPermission]
 * launchers up-front (one per [SayvaPermission] — the contract accepts one
 * permission string per launcher) and dispatches `request()` calls to the
 * matching launcher.
 *
 * `POST_NOTIFICATIONS` runtime permission only exists on API 33+. On older
 * versions we short-circuit the callback with `granted = true` to match
 * `PermissionController.isGranted`'s behavior — otherwise the UI would show
 * a permanent "Allow" chip that does nothing.
 */
@Composable
actual fun rememberPermissionRequester(
    onResult: (SayvaPermission, Boolean) -> Unit,
): PermissionRequester {
    // `rememberUpdatedState` so we always dispatch to the freshest callback
    // even if the caller passes a new lambda across recompositions. Without
    // this, we'd close over the launcher-registration-time callback.
    val currentOnResult by rememberUpdatedState(onResult)

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> currentOnResult(SayvaPermission.Camera, granted) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> currentOnResult(SayvaPermission.Microphone, granted) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> currentOnResult(SayvaPermission.Notifications, granted) }

    return remember(cameraLauncher, micLauncher, notifLauncher) {
        PermissionRequester { permission ->
            when (permission) {
                SayvaPermission.Camera -> cameraLauncher.launch(Manifest.permission.CAMERA)
                SayvaPermission.Microphone -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                SayvaPermission.Notifications -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        currentOnResult(SayvaPermission.Notifications, true)
                    } else {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }
}
