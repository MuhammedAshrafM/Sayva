package org.moashraf.sayva.data.prefs

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

/**
 * iOS implementation backed by a dedicated `NSUserDefaults` suite name
 * (`org.moashraf.sayva.settings`) so preferences namespace cleanly and don't
 * collide with the app's standard defaults or with third-party framework data.
 */
class IosSettingsStorage(suiteName: String = DEFAULT_SUITE) : SettingsStorage {

    private val settings = NSUserDefaultsSettings(
        NSUserDefaults(suiteName = suiteName) ?: NSUserDefaults.standardUserDefaults
    )

    override fun putString(key: String, value: String) { settings.putString(key, value) }
    override fun getString(key: String): String? =
        if (settings.hasKey(key)) settings.getString(key, "") else null

    override fun putBoolean(key: String, value: Boolean) { settings.putBoolean(key, value) }
    override fun getBoolean(key: String): Boolean? =
        if (settings.hasKey(key)) settings.getBoolean(key, false) else null

    override fun putInt(key: String, value: Int) { settings.putInt(key, value) }
    override fun getInt(key: String): Int? =
        if (settings.hasKey(key)) settings.getInt(key, 0) else null

    override fun putLong(key: String, value: Long) { settings.putLong(key, value) }
    override fun getLong(key: String): Long? =
        if (settings.hasKey(key)) settings.getLong(key, 0L) else null

    override fun putFloat(key: String, value: Float) { settings.putFloat(key, value) }
    override fun getFloat(key: String): Float? =
        if (settings.hasKey(key)) settings.getFloat(key, 0f) else null

    override fun remove(key: String) { settings.remove(key) }
    override fun clear() { settings.clear() }
    override fun contains(key: String): Boolean = settings.hasKey(key)

    companion object {
        const val DEFAULT_SUITE = "org.moashraf.sayva.settings"
    }
}

actual object SettingsStorageProvider {
    actual fun create(): SettingsStorage = IosSettingsStorage()
}
