package org.moashraf.sayva.data.prefs

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import org.moashraf.sayva.bootstrap.AndroidAppContext

/**
 * Android implementation backed by a dedicated `SharedPreferences` file
 * (`sayva_settings`), separate from the default one so uninstall-clean semantics
 * are predictable and other libraries can't accidentally collide on keys.
 */
class AndroidSettingsStorage private constructor(
    private val settings: SharedPreferencesSettings,
) : SettingsStorage {

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
        private const val FILE_NAME = "sayva_settings"

        fun create(context: Context): AndroidSettingsStorage {
            val prefs = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
            return AndroidSettingsStorage(SharedPreferencesSettings(prefs))
        }
    }
}

actual object SettingsStorageProvider {
    actual fun create(): SettingsStorage =
        AndroidSettingsStorage.create(AndroidAppContext.require())
}
