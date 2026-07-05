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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import org.moashraf.sayva.languagepack.LanguagePackController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.data.repository.DisplayMode
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.ErrorContainer
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.components.SayvaTopBar
import org.moashraf.sayva.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(nav: SayvaNavController) {
    val viewModel: SettingsViewModel = koinInject()
    val state by viewModel.state.collectAsState()
    val packState by viewModel.packState.collectAsState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        SayvaTopBar(
            onBack = { nav.back() },
            title = "Settings",
            modifier = Modifier.padding(horizontal = 8.dp),
            trailing = {
                Box(
                    modifier = Modifier.size(38.dp).background(SurfaceContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "search", size = 18.dp, color = OnSurface)
                }
            },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item { SettingsSectionLabel("DISPLAY") }
            item {
                SettingsRow(icon = "dark_mode", iconColor = Primary40, title = "Dark mode") {
                    Row(
                        modifier = Modifier
                            .background(SurfaceContainer, RoundedCornerShape(100))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DisplayMode.entries.forEach { option ->
                            val selected = option == state.displayMode
                            Text(
                                option.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else OnSurfaceVariant,
                                modifier = Modifier
                                    .clickable { viewModel.onDisplayModeChange(option) }
                                    .background(if (selected) Primary40 else Color.Transparent, RoundedCornerShape(100))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            item {
                val label = fontSizeLabel(state.fontSizeScale)
                SettingsRow(icon = "format_size", iconColor = Primary40, title = "Font size", subtitle = label) {
                    SettingsSliderTrack(
                        fraction = ((state.fontSizeScale - 0.8f) / 0.8f).coerceIn(0f, 1f),
                        color = Primary40,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }
            item {
                SettingsRow(icon = "contrast", iconColor = Primary40, title = "High contrast") {
                    Switch(
                        checked = state.highContrast,
                        onCheckedChange = viewModel::onHighContrastToggle,
                        colors = SwitchDefaults.colors(checkedTrackColor = Primary40),
                    )
                }
            }

            item { SettingsSectionLabel("LANGUAGE") }
            item {
                LanguagePackRows(
                    packState = packState,
                    onRecognitionSelected = { code ->
                        scope.launch { viewModel.onRecognitionLanguageChange(code) }
                    },
                    onOutputSelected = { code ->
                        scope.launch { viewModel.onOutputLanguageChange(code) }
                    },
                )
            }

            item { SettingsSectionLabel("SPEECH & SOUND") }
            item {
                SettingsRow(
                    icon = "record_voice_over",
                    iconColor = Secondary50,
                    title = "Voice",
                    subtitle = "Maya · Natural",
                    onClick = {},
                ) {
                    SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
                }
            }
            item {
                val speedLabel = "${state.voiceSpeed}× ${voiceSpeedTone(state.voiceSpeed)}"
                SettingsRow(icon = "speed", iconColor = Secondary50, title = "Speech speed", subtitle = speedLabel) {
                    SettingsSliderTrack(
                        fraction = ((state.voiceSpeed - 0.5f) / 1.5f).coerceIn(0f, 1f),
                        color = Secondary50,
                        modifier = Modifier.width(80.dp),
                    )
                }
            }

            item { SettingsSectionLabel("CAMERA & AI") }
            item {
                SettingsRow(
                    icon = "hd",
                    iconColor = Tertiary50,
                    title = "Camera quality",
                    subtitle = "Auto · saves battery",
                    onClick = {},
                ) {
                    SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
                }
            }
            item {
                SettingsRow(
                    icon = "cloud_off",
                    iconColor = Tertiary50,
                    title = "Offline mode",
                    subtitle = if (state.offlineMode) "Always · 100% on-device" else "Auto · uses cloud when needed",
                ) {
                    Switch(
                        checked = state.offlineMode,
                        onCheckedChange = viewModel::onOfflineModeToggle,
                        colors = SwitchDefaults.colors(checkedTrackColor = Tertiary50),
                    )
                }
            }
            item {
                SettingsRow(
                    icon = "cloud_download",
                    iconColor = Tertiary50,
                    title = "Offline models",
                    subtitle = "Manage downloaded language packs",
                    onClick = { nav.navigate(Screen.OfflineModels) },
                ) {
                    SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
                }
            }
            item {
                SettingsRow(
                    icon = "accessibility_new",
                    iconColor = Primary40,
                    title = "Accessibility",
                    subtitle = "Inclusive defaults",
                    onClick = { nav.navigate(Screen.Accessibility) },
                ) {
                    SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
                }
            }

            item { SettingsSectionLabel("DIAGNOSTICS") }
            item {
                SettingsRow(
                    icon = "bug_report",
                    iconColor = OnSurfaceVariant,
                    title = "System states",
                    subtitle = "Developer reference screen",
                    onClick = { nav.navigate(Screen.SystemStates) },
                ) {
                    SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
                }
            }
            item {
                SettingsRow(
                    icon = "error",
                    iconColor = ErrorColor,
                    title = "Report a problem",
                    subtitle = "Send diagnostics to engineering",
                    onClick = { nav.navigate(Screen.CrashReport) },
                ) {
                    SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(SurfaceContainer, RoundedCornerShape(12.dp))
                            .clickable {}
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Clear cache · 48 MB", style = MaterialTheme.typography.labelMedium, color = OnSurface)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(ErrorContainer, RoundedCornerShape(12.dp))
                            .clickable { viewModel.onResetToDefaults() }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Reset to defaults", style = MaterialTheme.typography.labelMedium, color = ErrorColor)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: String,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(name = icon, size = 20.dp, color = iconColor, filled = true)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant) }
        }
        trailing()
    }
}

@Composable
private fun SettingsSliderTrack(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(20.dp), contentAlignment = Alignment.CenterStart) {
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Outline, RoundedCornerShape(2.dp)))
        Box(modifier = Modifier.fillMaxWidth(fraction).height(4.dp).background(color, RoundedCornerShape(2.dp)))
    }
}

/** Label a font scale value: 0.8 → "Small", 1.0 → "Medium", 1.2+ → "Large". */
private fun fontSizeLabel(scale: Float): String = when {
    scale < 0.95f -> "Small"
    scale < 1.15f -> "Medium"
    else -> "Large"
}

/** "Slower / normal / faster" text label based on a voice-speed multiplier. */
private fun voiceSpeedTone(speed: Float): String = when {
    speed < 0.9f -> "slower"
    speed < 1.15f -> "normal"
    else -> "faster"
}

/**
 * Language Pack section — Recognition and Output rows.
 *
 * These are the two independent user-facing axes from the plan:
 *   * Recognition = which sign language the camera watches (active Pack)
 *   * Output       = which spoken language labels + TTS use
 *
 * When the pack subsystem is still bootstrapping (`Loading`) or errored,
 * we render placeholder rows so the section shape stays stable. On `Ready`,
 * the Recognition row shows the current pack (chevron menu suggests future
 * expansion; MVP has one installed pack), and the Output row is a segmented
 * control across the pack's `supportedOutputs`.
 *
 * Non-`complete` output languages display a small badge — that's how a user
 * discovers that Arabic labels are stubbed and gets a friendly "translation
 * preview" cue before they choose it.
 */
@Composable
private fun LanguagePackRows(
    packState: LanguagePackController.State,
    onRecognitionSelected: (String) -> Unit,
    onOutputSelected: (String) -> Unit,
) {
    Column {
        when (packState) {
            is LanguagePackController.State.Loading -> {
                SettingsRow(
                    icon = "translate",
                    iconColor = Secondary50,
                    title = "Recognition language",
                    subtitle = "Loading language packs…",
                ) {}
                SettingsRow(
                    icon = "record_voice_over",
                    iconColor = Secondary50,
                    title = "Output language",
                    subtitle = "Loading language packs…",
                ) {}
            }
            is LanguagePackController.State.Error -> {
                SettingsRow(
                    icon = "error",
                    iconColor = ErrorColor,
                    title = "Language packs unavailable",
                    subtitle = "Reinstall the app if this persists.",
                ) {}
            }
            is LanguagePackController.State.Ready -> {
                val currentPack = packState.currentPack
                val outputCode = packState.outputLanguage
                SettingsRow(
                    icon = "translate",
                    iconColor = Secondary50,
                    title = "Recognition language",
                    subtitle = currentPack.displayName(outputCode),
                    onClick = if (packState.availablePacks.size > 1) {
                        {
                            // MVP has one bundled pack. A dedicated Language Packs
                            // screen (Phase 4 — installs / manages downloaded ones)
                            // will host the picker; for now we defer navigation.
                        }
                    } else null,
                ) {
                    if (packState.availablePacks.size > 1) {
                        SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
                    } else {
                        // Single-pack MVP — surface the count so the user knows
                        // more will be available later.
                        Text(
                            "${packState.availablePacks.size} installed",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                        )
                    }
                }

                SettingsRow(
                    icon = "record_voice_over",
                    iconColor = Secondary50,
                    title = "Output language",
                    subtitle = outputLanguageSubtitle(currentPack, outputCode),
                ) {
                    Row(
                        modifier = Modifier
                            .background(SurfaceContainer, RoundedCornerShape(100))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        currentPack.supportedOutputs.forEach { code ->
                            val selected = code == outputCode
                            val label = code.uppercase()
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White else OnSurfaceVariant,
                                modifier = Modifier
                                    .clickable { onOutputSelected(code) }
                                    .background(
                                        if (selected) Secondary50 else Color.Transparent,
                                        RoundedCornerShape(100),
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Ignore lint about unused parameter until Phase 4 wires the picker.
                @Suppress("UNUSED_PARAMETER") val _unused = onRecognitionSelected
            }
        }
    }
}

private fun outputLanguageSubtitle(
    pack: org.moashraf.sayva.languagepack.LanguagePack,
    outputCode: String,
): String {
    val name = when (outputCode) {
        "en" -> "English"
        "ar" -> "العربية"
        else -> outputCode.uppercase()
    }
    val status = pack.statusOf(outputCode)?.name?.lowercase()
    return if (status != null && status != "complete") "$name · $status translations" else name
}
