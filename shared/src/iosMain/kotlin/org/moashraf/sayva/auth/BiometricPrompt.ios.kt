package org.moashraf.sayva.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorPasscodeNotSet
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorUserFallback
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import kotlin.coroutines.resume

/**
 * iOS implementation using LocalAuthentication (`LAContext`) with
 * `LAPolicyDeviceOwnerAuthenticationWithBiometrics` — Face ID or Touch ID only,
 * no passcode fallback. To allow passcode fallback, we'd switch to
 * `LAPolicyDeviceOwnerAuthentication`; deliberately keeping it biometric-only
 * to match the Android BIOMETRIC_STRONG behavior.
 */
class IosBiometricPrompt : BiometricPrompt {

    @OptIn(ExperimentalForeignApi::class)
    override fun availability(): BiometricAvailability {
        val context = LAContext()
        val canEvaluate = context.canEvaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            error = null,
        )
        if (canEvaluate) return BiometricAvailability.Available

        // canEvaluate returns false — inspect the error to know why.
        // We can't easily read the NSError without cinterop juggling; approximate.
        // For richer taxonomy, use canEvaluatePolicy(policy, error = &error) from Swift.
        return BiometricAvailability.NoneEnrolled
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun authenticate(
        title: String,
        subtitle: String?,
        cancelButtonText: String,
    ): BiometricResult = suspendCancellableCoroutine { cont ->
        val context = LAContext().apply {
            // localizedFallbackTitle set to empty string suppresses the "Enter Password"
            // fallback button — matching our biometric-only policy above.
            localizedFallbackTitle = ""
            localizedCancelTitle = cancelButtonText
        }
        val reason = if (subtitle.isNullOrBlank()) title else "$title\n$subtitle"

        context.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = reason,
            reply = { success, error ->
                if (!cont.isActive) return@evaluatePolicy
                if (success) {
                    cont.resume(BiometricResult.Success)
                } else {
                    val code = error?.code ?: 0L
                    val result = when (code) {
                        LAErrorUserCancel,
                        LAErrorUserFallback -> BiometricResult.UserCancelled

                        LAErrorAuthenticationFailed -> BiometricResult.Failed

                        LAErrorBiometryLockout,
                        LAErrorBiometryNotAvailable,
                        LAErrorBiometryNotEnrolled,
                        LAErrorPasscodeNotSet -> BiometricResult.Error("Biometry unavailable: $code")

                        else -> BiometricResult.Error(
                            error?.localizedDescription ?: "LAContext error code $code",
                        )
                    }
                    cont.resume(result)
                }
            },
        )

        cont.invokeOnCancellation { context.invalidate() }
    }
}

actual object BiometricPromptProvider {
    actual fun create(): BiometricPrompt = IosBiometricPrompt()
}
