package org.moashraf.sayva.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.moashraf.sayva.auth.AuthGateway
import org.moashraf.sayva.auth.User
import org.moashraf.sayva.db.SayvaDatabase
import org.moashraf.sayva.telemetry.AnalyticsGateway
import org.moashraf.sayva.telemetry.CrashReporter

/**
 * Bridges [AuthGateway] (canonical, cloud source of truth) with the local
 * `UserProfileEntity` cache (for fast cold-start rendering of `ProfileScreen`).
 *
 * ### Side effects on user change
 * - Local cache updated so the profile UI renders instantly on next launch
 *   without waiting for the auth gateway to fetch.
 * - Analytics + Crashlytics user id bound (or cleared on sign-out) so future
 *   events are attributable.
 *
 * These are all handled in the init block's collector — the interface stays
 * clean of side-effect concerns.
 */
class UserRepositoryImpl(
    private val authGateway: AuthGateway,
    private val database: SayvaDatabase,
    private val analytics: AnalyticsGateway,
    private val crashReporter: CrashReporter,
) : UserRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val currentUser: StateFlow<User?> = authGateway.currentUser

    init {
        // Mirror auth state → local cache + telemetry attribution.
        scope.launch {
            authGateway.currentUser.collect { user -> reconcile(user) }
        }
    }

    override fun snapshot(): User? = authGateway.currentUser.value

    override suspend fun signOut(): Result<Unit> {
        val result = authGateway.signOut()
        // Force-clear even if signOut failed — the user asked to sign out; we
        // shouldn't hold a stale session cached locally.
        clearLocalState()
        return result
    }

    override suspend fun updateDisplayName(newName: String): Result<Unit> {
        // Not yet wired to the gateway — Firebase supports `user.updateProfile`,
        // Supabase writes to user_metadata. Both live behind AuthGateway; we'll
        // add the method in a follow-up ticket.
        return Result.failure(
            NotImplementedError("updateDisplayName requires AuthGateway extension — deferred."),
        )
    }

    // -----------------------------------------------------------------

    private fun reconcile(user: User?) {
        if (user == null) {
            clearLocalState()
        } else {
            database.userProfileQueries.upsert(
                userId = user.id,
                email = user.email,
                displayName = user.displayName,
                isAnonymous = if (user.isAnonymous) 1L else 0L,
                isEmailVerified = if (user.isEmailVerified) 1L else 0L,
                isPlusSubscriber = 0L, // Populated by a separate entitlement fetch later.
                createdAt = Clock.System.now().toEpochMilliseconds(),
            )
            analytics.setUserId(user.id)
            crashReporter.setUserId(user.id)
        }
    }

    private fun clearLocalState() {
        database.userProfileQueries.clear()
        analytics.setUserId(null)
        crashReporter.setUserId(null)
    }
}
