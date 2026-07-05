package org.moashraf.sayva.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Provider-agnostic authentication port.
 *
 * Implementations live in `data/firebase/FirebaseAuthGateway.kt` and
 * `data/supabase/SupabaseAuthGateway.kt`. The Koin module in `di/Modules.kt`
 * picks one at startup based on [AppBackend.current].
 *
 * ### Design contract
 * - No vendor SDK type ever appears in this file or in any signature.
 * - All errors are normalized to [AuthError] variants by the adapter.
 * - Token refresh, session persistence, and platform-specific storage are
 *   implementation details owned by the adapter, not exposed on this interface.
 *
 * ### Cross-vendor satisfiability
 * Each method must be trivially implementable by BOTH backends:
 * - `signIn`      — Firebase: `signInWithEmailAndPassword`; Supabase: `auth.signInWith(Email) { email; password }`
 * - `register`    — Firebase: `createUserWithEmailAndPassword`; Supabase: `auth.signUpWith(Email) { email; password }`
 * - `sendPasswordReset` — Firebase: `sendPasswordResetEmail`; Supabase: `auth.resetPasswordForEmail`
 * - `signOut`     — Firebase: `signOut`; Supabase: `auth.signOut`
 * - `currentUser` — Firebase: `authStateChanges` (Flow); Supabase: `sessionStatus` (Flow)
 *
 * If a future method cannot be reasonably satisfied by one of the two, redesign the
 * method rather than adding an adapter that fakes it.
 */
interface AuthGateway {

    /**
     * Emits the currently signed-in [User] or `null` when signed out.
     * Hot flow — subscribers get the current value immediately on collection.
     */
    val currentUser: StateFlow<User?>

    /**
     * Attempt sign-in with email + password.
     * @return the authenticated [User] on success, or an [AuthError] variant on failure.
     */
    suspend fun signIn(email: String, password: String): Result<User>

    /**
     * Create a new account with email + password. Some providers auto-sign-in the
     * user on success (both Firebase and Supabase do); this is the observed behavior.
     */
    suspend fun register(email: String, password: String, displayName: String?): Result<User>

    /**
     * Request a password reset email be sent to [email]. Providers deliberately do NOT
     * reveal whether the email is registered — success just means "the request was
     * accepted," not "an email was actually sent."
     */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Sign out the current user. Idempotent — safe to call when already signed out. */
    suspend fun signOut(): Result<Unit>

    /**
     * Anonymous / guest session. Returns a [User] with `isAnonymous = true`.
     * Firebase supports this natively; Supabase supports it since 2024 via
     * `auth.signInAnonymously()`.
     */
    suspend fun signInAnonymously(): Result<User>
}

/**
 * Helper — adapters throw normalized [AuthError] variants; callers convert to
 * `Result` at the boundary. Keeps `suspend fun`s from having to declare thrown types.
 */
class AuthException(val error: AuthError) : RuntimeException(error.toString())
