package org.moashraf.sayva.data.secure

/**
 * Encrypted key-value storage for sensitive values (auth tokens, refresh tokens,
 * biometric-enrollment flags, session state).
 *
 * ### Platform backing
 * - **Android:** [EncryptedSharedPreferences] (from `androidx.security:security-crypto`)
 *   using an AES-256-GCM value scheme + AES-256-SIV key scheme, master key stored in the
 *   Android KeyStore.
 * - **iOS:** iOS Keychain via `multiplatform-settings`'s `KeychainSettings`, which uses
 *   `kSecClassGenericPassword` items scoped to the app's bundle identifier.
 *
 * ### Not appropriate for
 * - Large blobs (both backings assume small string values; use file storage for anything > 4 KB)
 * - Data that must survive app uninstall (both platforms clear on uninstall — that's by design)
 * - Data shared across users on the same device (both are per-app-per-user)
 *
 * ### Usage
 * Owned exclusively by auth adapters and other integrations that need token persistence.
 * ViewModels and screens must NOT access this directly — go through a repository.
 */
interface SecureStorage {

    /** Store or overwrite a string value under [key]. */
    fun putString(key: String, value: String)

    /** Retrieve the string stored under [key], or `null` if not present. */
    fun getString(key: String): String?

    /** Store or overwrite a boolean value under [key]. */
    fun putBoolean(key: String, value: Boolean)

    /** Retrieve the boolean stored under [key], or `null` if not present. */
    fun getBoolean(key: String): Boolean?

    /** Delete the entry for [key]. No-op if not present. */
    fun remove(key: String)

    /** Delete all entries. Used on sign-out and reset flows. */
    fun clear()

    /** True if [key] has a value stored. */
    fun contains(key: String): Boolean
}

/**
 * Platform factory. Implementations are provided in `androidMain` and `iosMain`.
 *
 * The Koin module in `di/PlatformModule.<platform>.kt` calls [create] to bind
 * the [SecureStorage] instance. See implementations for platform-specific
 * initialization requirements (Android needs a Context bootstrapped from
 * `MainActivity.onCreate`).
 */
expect object SecureStorageProvider {
    fun create(): SecureStorage
}
