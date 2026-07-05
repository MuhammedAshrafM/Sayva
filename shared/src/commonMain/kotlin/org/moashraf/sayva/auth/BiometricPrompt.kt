package org.moashraf.sayva.auth

/**
 * Platform-agnostic biometric authentication port.
 *
 * Used by the sign-in flow: after a user's first successful password sign-in,
 * we offer to enable biometric shortcut for future launches. On next launch,
 * if biometric is enabled, [authenticate] is called before any [AuthGateway]
 * calls to release the cached refresh token.
 *
 * ### Platforms
 * - **Android:** `androidx.biometric.BiometricPrompt` — Fingerprint, Face Unlock
 *   (Class 3 credentials only, per Android best practices).
 * - **iOS:** `LAContext.evaluatePolicy(deviceOwnerAuthenticationWithBiometrics)`
 *   — Face ID, Touch ID.
 *
 * ### Failure taxonomy
 * All non-success paths map to a [BiometricResult] variant. Adapters don't leak
 * platform exceptions upward — pattern-match on the result instead of catching.
 */
interface BiometricPrompt {

    /**
     * Whether biometric authentication is available on this device *right now*.
     *
     * Returns [BiometricAvailability.Available] if the user can authenticate.
     * Other variants indicate why not — no hardware, hardware disabled, no
     * enrolled credentials, etc.
     */
    fun availability(): BiometricAvailability

    /**
     * Show the platform biometric prompt and suspend until the user completes it.
     *
     * @param title             Shown in the prompt header. Keep short (~40 chars).
     * @param subtitle          Optional secondary line. Nullable.
     * @param cancelButtonText  Text for the cancel button — used only on Android;
     *                          iOS Face ID uses system-provided cancel.
     */
    suspend fun authenticate(
        title: String,
        subtitle: String? = null,
        cancelButtonText: String = "Cancel",
    ): BiometricResult
}

sealed class BiometricAvailability {
    /** Biometric is enrolled and ready. */
    data object Available : BiometricAvailability()

    /** Device has no biometric hardware. */
    data object NoHardware : BiometricAvailability()

    /** Hardware is present but temporarily unavailable (system update, etc.). */
    data object HardwareUnavailable : BiometricAvailability()

    /** No biometric credentials enrolled — user must set up in system settings. */
    data object NoneEnrolled : BiometricAvailability()

    /** Feature exists but disabled by policy (MDM, kids account, etc.). */
    data object BlockedByPolicy : BiometricAvailability()

    /** Any other unavailability. Diagnostic string only, not for user display. */
    data class Unknown(val cause: String) : BiometricAvailability()
}

sealed class BiometricResult {
    /** User authenticated successfully. */
    data object Success : BiometricResult()

    /** User cancelled the prompt (tapped Cancel or dismissed). */
    data object UserCancelled : BiometricResult()

    /** Authentication failed (wrong finger/face, too many attempts, etc.). */
    data object Failed : BiometricResult()

    /** System-level failure — lockout, hardware error. */
    data class Error(val cause: String) : BiometricResult()
}

/** Platform factory, resolved by Koin as `single<BiometricPrompt>`. */
expect object BiometricPromptProvider {
    fun create(): BiometricPrompt
}
