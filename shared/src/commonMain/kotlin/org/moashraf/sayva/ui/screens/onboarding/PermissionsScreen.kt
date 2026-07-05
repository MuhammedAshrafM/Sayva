package org.moashraf.sayva.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OnPrimaryContainer
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.permission.PermissionController
import org.moashraf.sayva.permission.SayvaPermission
import org.moashraf.sayva.permission.rememberPermissionRequester
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway
import org.moashraf.sayva.ui.components.PrimaryButton

@Composable
fun PermissionsScreen(nav: SayvaNavController) {
    val controller: PermissionController = koinInject()
    val analytics: AnalyticsGateway = koinInject()

    // Live grant state per permission. Populated once on entry, then updated
    // by the request-callback whenever the system dialog resolves. Values are
    // `null` until first checked so the UI can distinguish "loading" from
    // "denied" — mostly matters on iOS's async notifications check.
    val grants = remember { mutableStateMapOf<SayvaPermission, Boolean?>() }
    // Bump to force a re-poll after `openAppSettings()` — the user may have
    // toggled a permission in Settings and returned. On both platforms
    // returning from Settings recomposes eventually, but explicit poll is safer.
    var refreshTick by remember { mutableStateOf(0) }

    LaunchedEffect(refreshTick) {
        SayvaPermission.entries.forEach { permission ->
            grants[permission] = controller.isGranted(permission)
        }
    }

    val requester = rememberPermissionRequester { permission, granted ->
        grants[permission] = granted
        analytics.logEvent(
            AnalyticsEvents.PERMISSION_RESULT,
            mapOf(
                AnalyticsEvents.Param.PERMISSION to permission.name.lowercase(),
                AnalyticsEvents.Param.GRANTED to granted,
            ),
        )
    }

    fun handle(permission: SayvaPermission) {
        // If the current status is a definitive "denied", the OS won't show
        // the dialog again on Android's "don't ask again" or iOS's second
        // denial — punt them to Settings instead. On first-time "not yet
        // asked" (null / false with never-requested), the dialog will show.
        // We can't distinguish those states reliably from isGranted alone, so
        // we always try the launcher first — the callback resolves quickly
        // when there's no dialog to show, and the "Open Settings" fallback
        // covers the permanent-deny case via a long-press UX later.
        requester.request(permission)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.size(40.dp).padding(start = 8.dp, top = 4.dp).clickable { nav.back() },
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = "arrow_back", size = 20.dp, color = OnSurfaceVariant)
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 8.dp, bottom = 24.dp)) {
            Box(
                modifier = Modifier
                    .background(PrimaryContainer, RoundedCornerShape(100))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    "ONE LAST THING",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.padding(bottom = 14.dp))
            Text(
                "Just two things we'll need.",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
            )
            Text(
                "You can change these later in Settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurfaceVariant,
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PermissionCard(
                iconBg = PrimaryContainer,
                iconColor = Primary40,
                icon = "videocam",
                title = "Camera",
                subtitle = "Required · to see your signs",
                description = "Frames are processed on-device and never recorded.",
                granted = grants[SayvaPermission.Camera] == true,
                actionLabel = "Allow",
                onAction = { handle(SayvaPermission.Camera) },
            )
            PermissionCard(
                iconBg = SecondaryContainer,
                iconColor = Secondary50,
                icon = "mic",
                title = "Microphone",
                subtitle = "Optional · for voice → text",
                description = "Hear hearing people in conversation mode.",
                granted = grants[SayvaPermission.Microphone] == true,
                actionLabel = "Allow",
                onAction = { handle(SayvaPermission.Microphone) },
            )
            PermissionCard(
                iconBg = TertiaryContainer,
                iconColor = Tertiary50,
                icon = "notifications",
                title = "Notifications",
                subtitle = "Optional · streak reminders",
                description = null,
                granted = grants[SayvaPermission.Notifications] == true,
                actionLabel = "Allow",
                onAction = { handle(SayvaPermission.Notifications) },
            )
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(SurfaceContainer, RoundedCornerShape(12.dp))
                .clickable {
                    controller.openAppSettings()
                    // On return, re-poll to reflect any user-side changes.
                    refreshTick += 1
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "verified_user", size = 18.dp, color = Primary40, filled = true)
            Spacer(Modifier.width(10.dp))
            Text(
                "Open system settings to change grants",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            SymbolIcon(name = "open_in_new", size = 16.dp, color = OnSurfaceVariant)
        }

        PrimaryButton(
            text = "Continue",
            onClick = { nav.replaceAll(Screen.Login) },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
    }
}

@Composable
private fun PermissionCard(
    iconBg: Color,
    iconColor: Color,
    icon: String,
    title: String,
    subtitle: String,
    description: String?,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Outline, RoundedCornerShape(20.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(iconBg, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = icon, size = 26.dp, color = iconColor, filled = true)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            if (granted) {
                SymbolIcon(name = "check_circle", size = 24.dp, color = Tertiary50, filled = true)
            } else {
                Box(
                    modifier = Modifier
                        .background(Primary40, RoundedCornerShape(100))
                        .clickable { onAction() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }
        if (description != null) {
            Spacer(Modifier.padding(top = 12.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
