package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.moashraf.sayva.auth.User
import org.moashraf.sayva.data.repository.UserRepository
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway

/**
 * Feeds `ProfileScreen`. Reads the currently-signed-in [User] from
 * [UserRepository] (which itself bridges [org.moashraf.sayva.auth.AuthGateway]
 * with the local UserProfile cache).
 *
 * ### Sign-out flow
 * [signOut] delegates to the repository, which clears local state and the
 * gateway session. The caller is responsible for navigating back to Welcome —
 * the ViewModel doesn't own the NavController.
 */
class ProfileViewModel(
    private val repository: UserRepository,
    private val analytics: AnalyticsGateway,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Current user, or `null` if signed out. Hot flow. */
    val currentUser: StateFlow<User?> = repository.currentUser

    fun signOut() {
        // Log before the actual sign-out — once the auth session clears, the
        // analytics user id resets to null and any late-arriving event would
        // land on the anonymous bucket.
        analytics.logEvent(AnalyticsEvents.AUTH_SIGN_OUT)
        scope.launch { repository.signOut() }
    }
}
