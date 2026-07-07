package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.flow.StateFlow
import org.moashraf.sayva.data.repository.CameraQuality
import org.moashraf.sayva.data.repository.ColorBlindMode
import org.moashraf.sayva.data.repository.DisplayMode
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.data.repository.SettingsState
import org.moashraf.sayva.languagepack.LanguagePackController
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway

/**
 * Thin wrapper around [SettingsRepository]. Screens (`SettingsScreen`,
 * `AccessibilityScreen`) subscribe to [state] and call the typed setters below
 * on user actions.
 *
 * ### Why not extend androidx.lifecycle.ViewModel
 * The multiplatform `lifecycle-viewmodel-compose` port has been juggling
 * classpath boundaries across versions — for now we avoid the inheritance and
 * keep this as a plain class registered as a Koin `single`. Since we're not
 * using `viewModelScope`, we don't lose anything. When the multiplatform ViewModel
 * base is stable and per-screen scoping is needed (e.g. NavBackStackEntry-scoped
 * feature ViewModels), migrate then.
 *
 * ### Why this class exists at all
 * - stable seam for future work: analytics on setting changes, validation
 *   (e.g. clamp font-size slider to safe range), server-sync toggles.
 * - screens depend on this type, not `SettingsRepository` directly, so
 *   refactoring the underlying repo can happen without touching UI.
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val analytics: AnalyticsGateway,
    private val packController: LanguagePackController,
) {

    val state: StateFlow<SettingsState> = repository.state

    /**
     * The language pack subsystem exposes its own state (Loading → Ready →
     * Error) separately from `SettingsState`. The Settings screen observes
     * both and shows pack pickers only when [packState] is `Ready`. During
     * `Loading` (first-run bootstrap) we render a placeholder row.
     */
    val packState: StateFlow<LanguagePackController.State> = packController.state

    fun onDisplayModeChange(mode: DisplayMode) {
        repository.setDisplayMode(mode); log("display_mode", mode.name)
    }
    fun onFontSizeScaleChange(scale: Float) {
        repository.setFontSizeScale(scale); log("font_size_scale", scale)
    }
    fun onHighContrastToggle(enabled: Boolean) {
        repository.setHighContrast(enabled); log("high_contrast", enabled)
    }
    fun onVoiceSpeedChange(speed: Float) {
        repository.setVoiceSpeed(speed); log("voice_speed", speed)
    }
    fun onCameraQualityChange(quality: CameraQuality) {
        repository.setCameraQuality(quality); log("camera_quality", quality.name)
    }
    fun onOfflineModeToggle(enabled: Boolean) {
        repository.setOfflineMode(enabled); log("offline_mode", enabled)
    }
    fun onEmergencyModeToggle(enabled: Boolean) {
        repository.setEmergencyMode(enabled); log("emergency_mode", enabled)
    }

    fun onEasyModeToggle(enabled: Boolean) {
        repository.setEasyMode(enabled); log("easy_mode", enabled)
    }
    fun onLargerTextToggle(enabled: Boolean) {
        repository.setLargerText(enabled); log("larger_text", enabled)
    }
    fun onColorBlindModeChange(mode: ColorBlindMode) {
        repository.setColorBlindMode(mode); log("color_blind_mode", mode.name)
    }
    fun onLeftHandedToggle(enabled: Boolean) {
        repository.setLeftHandedMode(enabled); log("left_handed", enabled)
    }
    fun onHapticIntensityChange(intensity: Float) {
        repository.setHapticIntensity(intensity); log("haptic_intensity", intensity)
    }
    fun onReduceMotionToggle(enabled: Boolean) {
        repository.setReduceMotion(enabled); log("reduce_motion", enabled)
    }
    fun onScreenReaderHintsToggle(enabled: Boolean) {
        repository.setScreenReaderHints(enabled); log("screen_reader_hints", enabled)
    }
    fun onDeveloperModeToggle(enabled: Boolean) {
        repository.setDeveloperMode(enabled); log("developer_mode", enabled)
    }

    fun onResetToDefaults() {
        repository.resetToDefaults(); log("reset", "defaults")
    }

    /**
     * Switch the active recognition pack. Suspends because the controller
     * writes to `SettingsRepository` and (in Phase 2+) tears down the current
     * recognizer's native runtime before the switch commits.
     *
     * @return `true` if the switch succeeded. `false` when the controller
     *   isn't yet [LanguagePackController.State.Ready] or the code is unknown.
     */
    suspend fun onRecognitionLanguageChange(code: String): Boolean {
        val ok = packController.switchRecognition(code)
        if (ok) log("recognition_language", code)
        return ok
    }

    /**
     * Set the output language for the current pack. Falls back to the
     * pack's default output when the requested code isn't supported.
     */
    suspend fun onOutputLanguageChange(code: String): Boolean {
        val ok = packController.setOutputLanguage(code)
        if (ok) log("output_language", code)
        return ok
    }

    private fun log(key: String, value: Any) {
        analytics.logEvent(
            AnalyticsEvents.SETTING_CHANGED,
            mapOf(
                AnalyticsEvents.Param.KEY to key,
                AnalyticsEvents.Param.VALUE to value,
            ),
        )
    }
}
