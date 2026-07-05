package org.moashraf.sayva.data.secure

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings

/**
 * iOS implementation of [SecureStorage] backed by `KeychainSettings` from
 * `multiplatform-settings`. Under the hood this uses `kSecClassGenericPassword`
 * Keychain items scoped to the app's bundle identifier — accessible only to this
 * app, protected by the device's Secure Enclave when available.
 *
 * Items survive app updates but are wiped on uninstall (iOS behavior since iOS 10.3;
 * this is intentional and matches Android's KeyStore behavior).
 *
 * `service` is the Keychain service name — using our bundle-based value keeps
 * items namespaced so they don't collide with other apps or with our own future
 * additions (e.g., a separate `sayva.tokens` scope).
 */
@OptIn(ExperimentalSettingsImplementation::class)
class IosSecureStorage(service: String = DEFAULT_SERVICE) : SecureStorage {

    private val settings = KeychainSettings(service)

    override fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    override fun getString(key: String): String? =
        if (settings.hasKey(key)) settings.getString(key, "") else null

    override fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }

    override fun getBoolean(key: String): Boolean? =
        if (settings.hasKey(key)) settings.getBoolean(key, false) else null

    override fun remove(key: String) {
        settings.remove(key)
    }

    override fun clear() {
        settings.clear()
    }

    override fun contains(key: String): Boolean = settings.hasKey(key)

    companion object {
        const val DEFAULT_SERVICE = "org.moashraf.sayva.secure"
    }
}

actual object SecureStorageProvider {
    actual fun create(): SecureStorage = IosSecureStorage()
}
