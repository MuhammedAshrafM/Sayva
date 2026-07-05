package org.moashraf.sayva.data.prefs

/**
 * Non-secret key-value storage for user preferences and app state.
 *
 * ### When to use vs. [org.moashraf.sayva.data.secure.SecureStorage]
 * - **This** (`SettingsStorage`) — theme, font size, dark mode, feature flags,
 *   accessibility toggles, streak counters, onboarding-seen flags. Fast, unencrypted.
 * - **`SecureStorage`** — auth tokens, refresh tokens, biometric state. Encrypted,
 *   slower, uses OS-provided key material.
 *
 * ### Platform backing
 * - **Android:** `SharedPreferences` via `multiplatform-settings`
 * - **iOS:** `NSUserDefaults` via `multiplatform-settings`
 *
 * Both survive app updates, are wiped on uninstall, and are scoped per-user-per-app.
 */
interface SettingsStorage {

    fun putString(key: String, value: String)
    fun getString(key: String): String?

    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String): Boolean?

    fun putInt(key: String, value: Int)
    fun getInt(key: String): Int?

    fun putLong(key: String, value: Long)
    fun getLong(key: String): Long?

    fun putFloat(key: String, value: Float)
    fun getFloat(key: String): Float?

    fun remove(key: String)
    fun clear()
    fun contains(key: String): Boolean
}

/**
 * Platform factory. Implementations are provided in `androidMain` and `iosMain`.
 * Bound in Koin as a `single<SettingsStorage>` (see `di/Modules.kt`).
 */
expect object SettingsStorageProvider {
    fun create(): SettingsStorage
}
