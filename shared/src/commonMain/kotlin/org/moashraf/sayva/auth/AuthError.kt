package org.moashraf.sayva.auth

/**
 * Normalized error taxonomy for authentication failures.
 *
 * Both `FirebaseAuthGateway` and `SupabaseAuthGateway` translate their vendor-specific
 * exceptions into these variants at the boundary. This is the reconciliation point
 * between the two backends' incompatible error models — if you can't map a vendor
 * error to one of these variants, add a variant here rather than leaking the vendor
 * type upward.
 *
 * ViewModels pattern-match on these and never see vendor exceptions.
 */
sealed class AuthError {

    /** Wrong email/password combination. */
    data object InvalidCredentials : AuthError()

    /** Email is well-formed but no account exists for it. */
    data object UserNotFound : AuthError()

    /** Registration attempted with an email that already has an account. */
    data object EmailAlreadyInUse : AuthError()

    /** Password does not meet the provider's strength requirements. */
    data object WeakPassword : AuthError()

    /** Email string is malformed (fails RFC validation before submission). */
    data object InvalidEmail : AuthError()

    /** Provider rejected the request as too frequent. Client should back off. */
    data object TooManyRequests : AuthError()

    /** Network-level failure — no connection, DNS failure, timeout. */
    data object NetworkError : AuthError()

    /**
     * Any other failure. Message is diagnostic-only, never for user display —
     * screens should show a generic "Something went wrong" for `Unknown`.
     */
    data class Unknown(val cause: String) : AuthError()
}
