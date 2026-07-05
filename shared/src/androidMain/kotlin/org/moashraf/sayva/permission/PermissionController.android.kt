package org.moashraf.sayva.permission

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import org.moashraf.sayva.bootstrap.AndroidActivityProvider
import org.moashraf.sayva.bootstrap.AndroidAppContext

/**
 * Android implementation.
 *
 * Checks use [ContextCompat.checkSelfPermission] against the Application context.
 * Notifications: `POST_NOTIFICATIONS` runtime permission only exists on API 33+
 * (Android 13); on older versions notifications are granted-by-manifest.
 *
 * `openAppSettings` prefers using the current [AndroidActivityProvider] Activity
 * if one is available (so the launched Intent participates in the task stack);
 * falls back to the Application context with FLAG_ACTIVITY_NEW_TASK.
 */
class AndroidPermissionController : PermissionController {

    override suspend fun isGranted(permission: SayvaPermission): Boolean {
        val context = AndroidAppContext.require()
        val manifestPermission = when (permission) {
            SayvaPermission.Camera -> Manifest.permission.CAMERA
            SayvaPermission.Microphone -> Manifest.permission.RECORD_AUDIO
            SayvaPermission.Notifications -> {
                // Runtime permission only exists on Android 13+; on older
                // versions notifications are granted at install.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
                Manifest.permission.POST_NOTIFICATIONS
            }
        }
        return ContextCompat.checkSelfPermission(context, manifestPermission) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun openAppSettings() {
        val activity = AndroidActivityProvider.current()
        val context = activity ?: AndroidAppContext.require()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

actual object PermissionControllerProvider {
    actual fun create(): PermissionController = AndroidPermissionController()
}
