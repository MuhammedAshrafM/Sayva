package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.moashraf.sayva.data.prefs.SettingsStorage

/**
 * Concrete [SettingsRepository] backed by [SettingsStorage] (SharedPreferences /
 * NSUserDefaults).
 *
 * All reads are eager — [SettingsStorage] is fast, and eager reads keep the
 * [StateFlow] contract simple (subscribers see the current value on collect).
 * On write, we update the underlying key AND emit the new composite state, so
 * one collector re-renders once per user action.
 */
class SettingsRepositoryImpl(
    private val storage: SettingsStorage,
) : SettingsRepository {

    private val _state = MutableStateFlow(loadFromStorage())
    override val state: StateFlow<SettingsState> = _state.asStateFlow()

    override fun setDisplayMode(mode: DisplayMode) = update {
        storage.putString(K_DISPLAY_MODE, mode.name)
        it.copy(displayMode = mode)
    }

    override fun setFontSizeScale(scale: Float) = update {
        storage.putFloat(K_FONT_SIZE_SCALE, scale)
        it.copy(fontSizeScale = scale)
    }

    override fun setHighContrast(enabled: Boolean) = update {
        storage.putBoolean(K_HIGH_CONTRAST, enabled)
        it.copy(highContrast = enabled)
    }

    override fun setVoiceSpeed(speed: Float) = update {
        storage.putFloat(K_VOICE_SPEED, speed)
        it.copy(voiceSpeed = speed)
    }

    override fun setCameraQuality(quality: CameraQuality) = update {
        storage.putString(K_CAMERA_QUALITY, quality.name)
        it.copy(cameraQuality = quality)
    }

    override fun setOfflineMode(enabled: Boolean) = update {
        storage.putBoolean(K_OFFLINE_MODE, enabled)
        it.copy(offlineMode = enabled)
    }

    override fun setEmergencyMode(enabled: Boolean) = update {
        storage.putBoolean(K_EMERGENCY_MODE, enabled)
        it.copy(emergencyMode = enabled)
    }

    override fun setRecognitionLanguageCode(code: String?) = update {
        if (code == null) storage.remove(K_RECOGNITION_LANGUAGE)
        else storage.putString(K_RECOGNITION_LANGUAGE, code)
        it.copy(recognitionLanguageCode = code)
    }

    override fun setOutputLanguageCode(code: String?) = update {
        if (code == null) storage.remove(K_OUTPUT_LANGUAGE)
        else storage.putString(K_OUTPUT_LANGUAGE, code)
        it.copy(outputLanguageCode = code)
    }

    override fun setOnboardingCompleted(completed: Boolean) = update {
        storage.putBoolean(K_ONBOARDING_COMPLETED, completed)
        it.copy(onboardingCompleted = completed)
    }

    override fun setEasyMode(enabled: Boolean) = update {
        storage.putBoolean(K_EASY_MODE, enabled)
        it.copy(easyMode = enabled)
    }

    override fun setLargerText(enabled: Boolean) = update {
        storage.putBoolean(K_LARGER_TEXT, enabled)
        it.copy(largerText = enabled)
    }

    override fun setColorBlindMode(mode: ColorBlindMode) = update {
        storage.putString(K_COLOR_BLIND, mode.name)
        it.copy(colorBlindMode = mode)
    }

    override fun setLeftHandedMode(enabled: Boolean) = update {
        storage.putBoolean(K_LEFT_HANDED, enabled)
        it.copy(leftHandedMode = enabled)
    }

    override fun setHapticIntensity(intensity: Float) = update {
        storage.putFloat(K_HAPTIC, intensity)
        it.copy(hapticIntensity = intensity)
    }

    override fun setReduceMotion(enabled: Boolean) = update {
        storage.putBoolean(K_REDUCE_MOTION, enabled)
        it.copy(reduceMotion = enabled)
    }

    override fun setScreenReaderHints(enabled: Boolean) = update {
        storage.putBoolean(K_SCREEN_READER_HINTS, enabled)
        it.copy(screenReaderHints = enabled)
    }

    override fun setDeveloperMode(enabled: Boolean) = update {
        storage.putBoolean(K_DEVELOPER_MODE, enabled)
        it.copy(developerMode = enabled)
    }

    override fun resetToDefaults() {
        // Clear only the keys we own — don't clobber other consumers of the
        // same SettingsStorage instance (there aren't any today, but stay safe).
        listOf(
            K_DISPLAY_MODE, K_FONT_SIZE_SCALE, K_HIGH_CONTRAST, K_VOICE_SPEED,
            K_CAMERA_QUALITY, K_OFFLINE_MODE, K_EMERGENCY_MODE, K_EASY_MODE,
            K_LARGER_TEXT, K_COLOR_BLIND, K_LEFT_HANDED, K_HAPTIC,
            K_REDUCE_MOTION, K_SCREEN_READER_HINTS,
            K_RECOGNITION_LANGUAGE, K_OUTPUT_LANGUAGE,
            K_ONBOARDING_COMPLETED, K_DEVELOPER_MODE,
        ).forEach(storage::remove)
        _state.value = SettingsState()
    }

    private inline fun update(mutation: (SettingsState) -> SettingsState) {
        _state.value = mutation(_state.value)
    }

    private fun loadFromStorage(): SettingsState {
        val defaults = SettingsState()
        return SettingsState(
            displayMode = storage.getString(K_DISPLAY_MODE)
                ?.let { runCatching { DisplayMode.valueOf(it) }.getOrNull() }
                ?: defaults.displayMode,
            fontSizeScale = storage.getFloat(K_FONT_SIZE_SCALE) ?: defaults.fontSizeScale,
            highContrast = storage.getBoolean(K_HIGH_CONTRAST) ?: defaults.highContrast,
            voiceSpeed = storage.getFloat(K_VOICE_SPEED) ?: defaults.voiceSpeed,
            cameraQuality = storage.getString(K_CAMERA_QUALITY)
                ?.let { runCatching { CameraQuality.valueOf(it) }.getOrNull() }
                ?: defaults.cameraQuality,
            offlineMode = storage.getBoolean(K_OFFLINE_MODE) ?: defaults.offlineMode,
            emergencyMode = storage.getBoolean(K_EMERGENCY_MODE) ?: defaults.emergencyMode,
            easyMode = storage.getBoolean(K_EASY_MODE) ?: defaults.easyMode,
            largerText = storage.getBoolean(K_LARGER_TEXT) ?: defaults.largerText,
            colorBlindMode = storage.getString(K_COLOR_BLIND)
                ?.let { runCatching { ColorBlindMode.valueOf(it) }.getOrNull() }
                ?: defaults.colorBlindMode,
            leftHandedMode = storage.getBoolean(K_LEFT_HANDED) ?: defaults.leftHandedMode,
            hapticIntensity = storage.getFloat(K_HAPTIC) ?: defaults.hapticIntensity,
            reduceMotion = storage.getBoolean(K_REDUCE_MOTION) ?: defaults.reduceMotion,
            screenReaderHints = storage.getBoolean(K_SCREEN_READER_HINTS)
                ?: defaults.screenReaderHints,
            recognitionLanguageCode = storage.getString(K_RECOGNITION_LANGUAGE),
            outputLanguageCode = storage.getString(K_OUTPUT_LANGUAGE),
            onboardingCompleted = storage.getBoolean(K_ONBOARDING_COMPLETED)
                ?: defaults.onboardingCompleted,
            developerMode = storage.getBoolean(K_DEVELOPER_MODE)
                ?: defaults.developerMode,
        )
    }

    private companion object Keys {
        const val K_DISPLAY_MODE = "display.mode"
        const val K_FONT_SIZE_SCALE = "display.fontSizeScale"
        const val K_HIGH_CONTRAST = "display.highContrast"
        const val K_VOICE_SPEED = "speech.voiceSpeed"
        const val K_CAMERA_QUALITY = "camera.quality"
        const val K_OFFLINE_MODE = "camera.offlineMode"
        const val K_EMERGENCY_MODE = "favorites.emergencyMode"
        const val K_EASY_MODE = "a11y.easyMode"
        const val K_LARGER_TEXT = "a11y.largerText"
        const val K_COLOR_BLIND = "a11y.colorBlindMode"
        const val K_LEFT_HANDED = "a11y.leftHanded"
        const val K_HAPTIC = "a11y.hapticIntensity"
        const val K_REDUCE_MOTION = "a11y.reduceMotion"
        const val K_SCREEN_READER_HINTS = "a11y.screenReaderHints"
        const val K_RECOGNITION_LANGUAGE = "languagepack.recognitionCode"
        const val K_OUTPUT_LANGUAGE = "languagepack.outputCode"
        const val K_ONBOARDING_COMPLETED = "onboarding.completed"
        const val K_DEVELOPER_MODE = "developer.mode"
    }
}
