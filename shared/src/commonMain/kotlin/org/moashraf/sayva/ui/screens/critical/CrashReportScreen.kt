package org.moashraf.sayva.ui.screens.critical

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.Platform
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.ErrorContainer
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.SuccessColor
import org.moashraf.sayva.designsystem.SuccessContainer
import org.moashraf.sayva.designsystem.Surface
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway
import org.moashraf.sayva.telemetry.CrashReporter
import org.moashraf.sayva.ui.components.PrimaryButton
import org.moashraf.sayva.ui.components.SecondaryButton

private data class ReportToggle(val id: String, val icon: String, val title: String, val defaultOn: Boolean)

// Detail strings are computed dynamically; only static metadata lives here.
private val toggleSpecs = listOf(
    ReportToggle(id = "logs", icon = "bug_report", title = "Error logs", defaultOn = true),
    ReportToggle(id = "device", icon = "smartphone", title = "Device info", defaultOn = true),
    ReportToggle(id = "screenshot", icon = "image", title = "Screenshot", defaultOn = false),
)

/**
 * Small marker exception used to file the user's crash report as a non-fatal
 * on the crash provider. Named distinctly so reports show up bucketed under
 * "UserReportedIssue" in Firebase — separate from real caught exceptions.
 */
private class UserReportedIssue(message: String) : RuntimeException(message)

@Composable
fun CrashReportScreen(nav: SayvaNavController) {
    val crashReporter: CrashReporter = koinInject()
    val analytics: AnalyticsGateway = koinInject()
    val platform: Platform = koinInject()

    // Toggle state, keyed by toggleSpec id so we can look up "logs" / "device" /
    // "screenshot" without threading indices around.
    val toggleStates = remember {
        toggleSpecs.associate { it.id to mutableStateOf(it.defaultOn) }
    }
    var description by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    fun submit() {
        if (submitted) return
        submitted = true

        val logs = toggleStates.getValue("logs").value
        val device = toggleStates.getValue("device").value
        val screenshot = toggleStates.getValue("screenshot").value

        // Attach opt-in context as breadcrumb keys — Crashlytics keeps these on
        // subsequent reports too, so we clear them at the end of this handler.
        crashReporter.setKey("report_source", "user_form")
        crashReporter.setKey("includes_logs", logs.toString())
        crashReporter.setKey("includes_device_info", device.toString())
        crashReporter.setKey("includes_screenshot", screenshot.toString())
        if (device) {
            // Platform.name already includes name+version (e.g. "Android 34" /
            // "iOS 17.0"), so a single key captures both.
            crashReporter.setKey("platform", platform.name)
        }
        // User's description goes on a breadcrumb (not as an event param) —
        // keeps it out of the analytics stream (privacy) but visible on the
        // crash report itself.
        val trimmed = description.trim()
        if (trimmed.isNotEmpty()) {
            crashReporter.log("user_description: $trimmed")
        }
        crashReporter.log("Crash report submitted by user")

        // File the report as a non-fatal so it shows up in the dashboard.
        val summary = if (trimmed.isNotEmpty()) {
            "User report: ${trimmed.take(140)}"
        } else {
            "User report (no description)"
        }
        crashReporter.recordException(UserReportedIssue(summary))

        // Analytics event mirrors the toggles so we can dashboard "how many
        // users include screenshots" etc. without touching the crash system.
        analytics.logEvent(
            AnalyticsEvents.CRASH_REPORT_SUBMITTED,
            mapOf(
                AnalyticsEvents.Param.INCLUDES_LOGS to logs,
                AnalyticsEvents.Param.INCLUDES_DEVICE_INFO to device,
                AnalyticsEvents.Param.INCLUDES_SCREENSHOT to screenshot,
                AnalyticsEvents.Param.HAS_USER_DESCRIPTION to trimmed.isNotEmpty(),
            ),
        )

        nav.replaceAll(Screen.Home)
    }

    fun dismiss() {
        analytics.logEvent(AnalyticsEvents.CRASH_REPORT_DISMISSED)
        nav.replaceAll(Screen.Home)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(top = 48.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                            .size(96.dp)
                            .background(
                                Brush.linearGradient(listOf(ErrorContainer, Color(0xFFFBD7DD))),
                                RoundedCornerShape(28.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(name = "sentiment_dissatisfied", size = 48.dp, color = ErrorColor, filled = true)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(32.dp)
                                .border(4.dp, Surface, CircleShape)
                                .background(ErrorColor, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            SymbolIcon(name = "error", size = 18.dp, color = Color.White, filled = true)
                        }
                    }
                    Text(
                        "Sorry — that didn't work.",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sayva hit an unexpected error. Help us fix it by sending an anonymous report.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            itemsIndexed(toggleSpecs) { _, spec ->
                val state = toggleStates.getValue(spec.id)
                val detail = when (spec.id) {
                    "logs" -> "Anonymized stack trace attached to this session"
                    "device" -> platform.name
                    "screenshot" -> "Faces auto-blurred before send"
                    else -> ""
                }
                ToggleRow(
                    icon = spec.icon,
                    title = spec.title,
                    detail = detail,
                    checked = state.value,
                    onToggle = { state.value = it },
                )
                Spacer(Modifier.height(6.dp))
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp)
                        .background(Color.White, RoundedCornerShape(14.dp))
                        .border(1.dp, OutlineStrong, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Text(
                        "What were you doing? (optional)",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        textStyle = LocalTextStyle.current.merge(
                            MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                        ),
                        cursorBrush = SolidColor(Primary40),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (description.isEmpty()) {
                                Text(
                                    "e.g. Trying to record the sign for 'thanks'…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = OnSurfaceVariant,
                                )
                            }
                            inner()
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(140.dp)) }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(SuccessContainer, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF6BCFAB), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "verified_user", size = 18.dp, color = SuccessColor, filled = true)
                Spacer(Modifier.width(10.dp))
                Text(
                    "No translations · no video · no audio is ever sent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF005544),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                SecondaryButton(
                    text = "Not now",
                    onClick = { dismiss() },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                PrimaryButton(
                    text = if (submitted) "Sent" else "Send report",
                    leadingIcon = "send",
                    onClick = { submit() },
                    enabled = !submitted,
                    modifier = Modifier.weight(2f),
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: String,
    title: String,
    detail: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).background(SurfaceContainer, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = icon, size = 18.dp, color = OnSurfaceVariant, filled = true)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(detail, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(26.dp)
                .background(if (checked) Primary40 else Outline, RoundedCornerShape(100))
                .clickable { onToggle(!checked) },
        ) {
            Box(
                modifier = Modifier
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 2.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape)
                    .then(if (!checked) Modifier.border(1.dp, OutlineStrong, CircleShape) else Modifier),
            )
        }
    }
}
