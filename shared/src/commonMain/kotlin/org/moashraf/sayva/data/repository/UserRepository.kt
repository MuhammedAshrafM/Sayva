package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.moashraf.sayva.auth.User

/**
 * Consumer of `ProfileScreen`, `LoginScreen` post-sign-in, and anywhere the app
 * needs to know "who am I".
 *
 * Bridges two sources:
 * - [org.moashraf.sayva.auth.AuthGateway] — canonical, cloud-sourced. Truth.
 * - `UserProfileEntity` in SQLDelight — local cache for offline UI (avatar,
 *   name) so `ProfileScreen` renders instantly on cold start before auth
 *   round-trips.
 *
 * The two are reconciled inside the implementation. Callers see one [User] flow.
 */
interface UserRepository {

    /**
     * Currently-signed-in user, or `null` if signed out.
     * Hot flow — collectors see the current value immediately.
     */
    val currentUser: StateFlow<User?>

    /** Snapshot without subscribing. */
    fun snapshot(): User?

    /**
     * Sign out — delegates to AuthGateway.signOut() and clears the local
     * profile cache. Also clears any auth-bound analytics/crash user ids.
     */
    suspend fun signOut(): Result<Unit>

    /**
     * Update the local display name. Behavior depends on backend — Firebase
     * lets us push this through `updateProfile`; Supabase writes to
     * `user_metadata`. The gateway hides the difference.
     */
    suspend fun updateDisplayName(newName: String): Result<Unit>
}
