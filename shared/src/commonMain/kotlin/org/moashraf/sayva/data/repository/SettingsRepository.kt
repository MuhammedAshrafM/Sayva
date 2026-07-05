package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.StateFlow

/**
 * Consumer of `SettingsScreen` and `AccessibilityScreen`. All non-secret user
 * preferences.
 *
 * Persistence: backed by [org.moashraf.sayva.data.prefs.SettingsStorage] —
 * SharedPreferences on Android, NSUserDefaults on iOS.
 *
 * Design: one [SettingsState] object exposes all preferences; ViewModels observe
 * the whole thing and re-render on any change. Per-field mutations write both
 * the individual key and emit the updated composite. Simple, matches how the
 * UI actually consumes settings.
 */
interface SettingsRepository {

    /** All current settings values. Emits again on any change. */
    val state: StateFlow<SettingsState>

    fun setDisplayMode(mode: DisplayMode)
    fun setFontSizeScale(scale: Float)
    fun setHighContrast(enabled: Boolean)
    fun setVoiceSpeed(speed: Float)
    fun setCameraQuality(quality: CameraQuality)
    fun setOfflineMode(enabled: Boolean)
    fun setEmergencyMode(enabled: Boolean)

    // Language Pack selection — recognition + output are independent axes.
    // `null` means "app default" — the ActiveLanguagePack resolves to the
    // MVP bundled pack for recognition and to system locale (or pack's
    // defaultOutputLanguage) for output.
    fun setRecognitionLanguageCode(code: String?)
    fun setOutputLanguageCode(code: String?)

    /**
     * Set once the user has completed the initial onboarding flow
     * (Welcome → HowAiWorks → TwoWayIntro → Permissions → Login/Register).
     * Consumed by the startup coordinator — a returning user who signed out
     * lands on Login, not on Welcome, so they don't re-walk the tutorial.
     */
    fun setOnboardingCompleted(completed: Boolean)

    // Accessibility
    fun setEasyMode(enabled: Boolean)
    fun setLargerText(enabled: Boolean)
    fun setColorBlindMode(mode: ColorBlindMode)
    fun setLeftHandedMode(enabled: Boolean)
    fun setHapticIntensity(intensity: Float)
    fun setReduceMotion(enabled: Boolean)
    fun setScreenReaderHints(enabled: Boolean)

    /** Reset every setting to its default. */
    fun resetToDefaults()
}

/**
 * Snapshot of every setting. Defaults chosen so a fresh install looks right
 * without any writes.
 */
data class SettingsState(
    // Display
    val displayMode: DisplayMode = DisplayMode.Auto,
    val fontSizeScale: Float = 1.0f,
    val highContrast: Boolean = false,

    // Speech
    val voiceSpeed: Float = 1.0f,

    // Camera & AI
    val cameraQuality: CameraQuality = CameraQuality.HighDefinition,
    val offlineMode: Boolean = false,

    // Emergency mode toggle on FavoritesScreen
    val emergencyMode: Boolean = false,

    // Language Pack selection. `null` = "use app default" (MVP bundled pack;
    // system locale for output). ActiveLanguagePack resolves these into a
    // concrete pack + output code at read time.
    val recognitionLanguageCode: String? = null,
    val outputLanguageCode: String? = null,

    // `true` once the user has finished the first-run onboarding flow.
    // Read by the startup coordinator to decide the initial destination.
    val onboardingCompleted: Boolean = false,

    // Accessibility
    val easyMode: Boolean = false,
    val largerText: Boolean = false,
    val colorBlindMode: ColorBlindMode = ColorBlindMode.Off,
    val leftHandedMode: Boolean = false,
    val hapticIntensity: Float = 0.7f,
    val reduceMotion: Boolean = false,
    val screenReaderHints: Boolean = true,
)

enum class DisplayMode { Light, Auto, Dark }

enum class CameraQuality { Standard, HighDefinition }

enum class ColorBlindMode { Off, Deuteranopia, Protanopia, Tritanopia }
