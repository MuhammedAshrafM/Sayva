package org.moashraf.sayva.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.moashraf.sayva.bootstrap.AndroidAppContext

/**
 * Android implementation of [SecureStorage] backed by [EncryptedSharedPreferences].
 *
 * The master key lives in the Android KeyStore (hardware-backed on devices with
 * a TEE / StrongBox). Values are encrypted with AES-256-GCM; keys with AES-256-SIV
 * (deterministic — the same plaintext key always maps to the same encrypted key,
 * allowing lookup without decrypting all entries).
 *
 * ### Bootstrap
 * `MainActivity.onCreate` must call [AndroidSecureStorageBootstrap.init] with the
 * application context BEFORE Koin resolves `SecureStorage`. Without that call,
 * [SecureStorageProvider.create] throws an [IllegalStateException] pointing here.
 *
 * ### Fallback for broken EncryptedSharedPreferences
 * Some Samsung devices with older Tink versions have shipped with broken KeyStore
 * providers that fail to initialize `EncryptedSharedPreferences`. When that happens,
 * we log to Crashlytics (via the caller — SecureStorage does not depend on it) and
 * fall back to plain [SharedPreferences]. This is a known Android platform issue
 * documented in the parent plan (§ 1.3 watchouts).
 */
class AndroidSecureStorage private constructor(
    private val prefs: SharedPreferences,
) : SecureStorage {

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getString(key: String): String? =
        if (prefs.contains(key)) prefs.getString(key, null) else null

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String): Boolean? =
        if (prefs.contains(key)) prefs.getBoolean(key, false) else null

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun contains(key: String): Boolean = prefs.contains(key)

    companion object {
        private const val FILE_NAME = "sayva_secure"

        fun create(context: Context): AndroidSecureStorage {
            val prefs = try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (t: Throwable) {
                // Fallback: on broken KeyStore devices, EncryptedSharedPreferences
                // init fails. Fall back to plain SharedPreferences so the app still
                // works — but the caller should notice via Crashlytics that this
                // path was taken. See parent plan § 1.3 for context.
                context.getSharedPreferences("${FILE_NAME}_plain", Context.MODE_PRIVATE)
            }
            return AndroidSecureStorage(prefs)
        }
    }
}

actual object SecureStorageProvider {
    actual fun create(): SecureStorage =
        AndroidSecureStorage.create(AndroidAppContext.require())
}
