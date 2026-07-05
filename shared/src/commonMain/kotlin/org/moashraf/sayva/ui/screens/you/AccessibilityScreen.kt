package org.moashraf.sayva.ui.screens.you

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.data.repository.ColorBlindMode
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.viewmodel.SettingsViewModel

/** Short display labels for the color-blind mode chips. Keep tight to fit. */
private val colorBlindChoices = listOf(
    "Off" to ColorBlindMode.Off,
    "Deut." to ColorBlindMode.Deuteranopia,
    "Prot." to ColorBlindMode.Protanopia,
    "Trit." to ColorBlindMode.Tritanopia,
)

@Composable
fun AccessibilityScreen(nav: SayvaNavController) {
    val viewModel: SettingsViewModel = koinInject()
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clickable { nav.back() },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "arrow_back", size = 22.dp, color = OnSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                Text("Accessibility", style = MaterialTheme.typography.titleMedium)
                Text("Make Sayva yours.", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(Brush.linearGradient(listOf(Primary40, Secondary50)), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(name = "accessibility_new", size = 24.dp, color = Color.White, filled = true)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Easy mode", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(
                            "Bigger buttons · simpler layout · perfect for elders",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                    Switch(
                        checked = state.easyMode,
                        onCheckedChange = viewModel::onEasyModeToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color.White.copy(alpha = 0.4f),
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.White,
                        ),
                    )
                }
            }

            item {
                AccessibilityToggleRow(
                    icon = "text_increase",
                    title = "Larger text",
                    subtitle = "Body · 18 pt",
                    checked = state.largerText,
                    onCheckedChange = viewModel::onLargerTextToggle,
                )
            }
            item {
                AccessibilityToggleRow(
                    icon = "contrast",
                    title = "High contrast",
                    subtitle = "Pure black on white",
                    checked = state.highContrast,
                    onCheckedChange = viewModel::onHighContrastToggle,
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        SymbolIcon(name = "palette", size = 22.dp, color = Primary40, filled = true)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Color blind", style = MaterialTheme.typography.titleSmall)
                            Text(
                                colorBlindSubtitle(state.colorBlindMode),
                                style = MaterialTheme.typography.bodySmall,
                                color = OnSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        colorBlindChoices.forEach { (label, mode) ->
                            val selected = mode == state.colorBlindMode
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else OnSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.onColorBlindModeChange(mode) }
                                    .background(if (selected) Primary40 else SurfaceContainer, RoundedCornerShape(8.dp))
                                    .padding(vertical = 6.dp),
                            )
                        }
                    }
                }
            }
            item {
                AccessibilityToggleRow(
                    icon = "back_hand",
                    title = "Left-handed mode",
                    subtitle = "Mirror UI & default detection",
                    checked = state.leftHandedMode,
                    onCheckedChange = viewModel::onLeftHandedToggle,
                )
            }
            item {
                AccessibilityToggleRow(
                    icon = "vibration",
                    title = "Haptic feedback",
                    subtitle = hapticLabel(state.hapticIntensity),
                    checked = state.hapticIntensity > 0f,
                    onCheckedChange = { enabled ->
                        viewModel.onHapticIntensityChange(if (enabled) 0.7f else 0f)
                    },
                    trailingOverride = {
                        Box(modifier = Modifier.width(90.dp).height(20.dp), contentAlignment = Alignment.CenterStart) {
                            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Outline, RoundedCornerShape(2.dp)))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(state.hapticIntensity.coerceIn(0f, 1f))
                                    .height(4.dp)
                                    .background(Primary40, RoundedCornerShape(2.dp)),
                            )
                        }
                    },
                )
            }
            item {
                AccessibilityToggleRow(
                    icon = "motion_photos_off",
                    title = "Reduce motion",
                    subtitle = null,
                    checked = state.reduceMotion,
                    onCheckedChange = viewModel::onReduceMotionToggle,
                )
            }
            item {
                AccessibilityToggleRow(
                    icon = "screen_record",
                    title = "Screen reader hints",
                    subtitle = "TalkBack · BrailleBack ready",
                    checked = state.screenReaderHints,
                    onCheckedChange = viewModel::onScreenReaderHintsToggle,
                )
            }

            item {
                Spacer(Modifier.height(14.dp))
                Text(
                    buildString {
                        append("Beyond requirements · One-handed reach mode (UI bottom-anchored), low-vision sign avatar with thick outlines, ")
                        append("captions burned into the camera view, BSL/ASL deaf-blind tactile output preview, partner pairing for shared ")
                        append("captions on a second device.")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

private fun colorBlindSubtitle(mode: ColorBlindMode): String = when (mode) {
    ColorBlindMode.Off -> "Standard palette"
    ColorBlindMode.Deuteranopia -> "Deuteranopia palette"
    ColorBlindMode.Protanopia -> "Protanopia palette"
    ColorBlindMode.Tritanopia -> "Tritanopia palette"
}

private fun hapticLabel(intensity: Float): String = when {
    intensity <= 0f -> "Off"
    intensity < 0.4f -> "Light"
    intensity < 0.75f -> "Medium"
    else -> "Strong"
}

@Composable
private fun AccessibilityToggleRow(
    icon: String,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    trailingOverride: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(name = icon, size = 22.dp, color = Primary40, filled = true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) }
        }
        if (trailingOverride != null) {
            trailingOverride()
        } else {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Primary40),
            )
        }
    }
}
