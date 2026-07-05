package org.moashraf.sayva.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt as AndroidxBiometricPrompt
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import org.moashraf.sayva.bootstrap.AndroidActivityProvider
import org.moashraf.sayva.bootstrap.AndroidAppContext

/**
 * Android implementation using `androidx.biometric.BiometricPrompt` with
 * [BIOMETRIC_STRONG] authenticators only — Class 3 biometrics that satisfy
 * KeyStore-backed key requirements. Class 2 (weaker face unlock on some devices)
 * is deliberately excluded because it's not sufficient for cryptographic operations.
 */
class AndroidBiometricPrompt : BiometricPrompt {

    override fun availability(): BiometricAvailability {
        val manager = BiometricManager.from(AndroidAppContext.require())
        return when (manager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.BlockedByPolicy
            else -> BiometricAvailability.Unknown(cause = "canAuthenticate returned unexpected code")
        }
    }

    override suspend fun authenticate(
        title: String,
        subtitle: String?,
        cancelButtonText: String,
    ): BiometricResult = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val activity = AndroidActivityProvider.current()
            if (activity == null) {
                cont.resume(BiometricResult.Error("No FragmentActivity in foreground"))
                return@suspendCancellableCoroutine
            }

            val executor = ContextCompat.getMainExecutor(activity)
            val callback = object : AndroidxBiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: AndroidxBiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    // Called on individual attempt failure (wrong fingerprint) — the
                    // prompt stays open. Don't resume here; wait for either Success
                    // or a terminal Error.
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!cont.isActive) return
                    val res = when (errorCode) {
                        AndroidxBiometricPrompt.ERROR_USER_CANCELED,
                        AndroidxBiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        AndroidxBiometricPrompt.ERROR_CANCELED -> BiometricResult.UserCancelled

                        AndroidxBiometricPrompt.ERROR_LOCKOUT,
                        AndroidxBiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricResult.Error("Locked out")

                        else -> BiometricResult.Error("$errorCode: $errString")
                    }
                    cont.resume(res)
                }
            }

            val prompt = AndroidxBiometricPrompt(activity, executor, callback)
            val info = AndroidxBiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply { if (!subtitle.isNullOrBlank()) setSubtitle(subtitle) }
                .setNegativeButtonText(cancelButtonText)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .build()

            prompt.authenticate(info)
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}

actual object BiometricPromptProvider {
    actual fun create(): BiometricPrompt = AndroidBiometricPrompt()
}
