package org.moashraf.sayva.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.moashraf.sayva.auth.AuthError
import org.moashraf.sayva.auth.AuthException
import org.moashraf.sayva.auth.AuthGateway
import org.moashraf.sayva.auth.User

/**
 * Firebase implementation of [AuthGateway]. **Only file** in the codebase that
 * imports `dev.gitlive.firebase.auth.*` types — the rest of the app talks to
 * the [AuthGateway] interface.
 *
 * Error translation from Firebase's exception hierarchy to [AuthError] happens
 * inside [mapError]. Failures come back as `Result.failure(AuthException(error))`
 * so ViewModels can pattern-match on `AuthException.error` without any Firebase
 * type crossing the boundary.
 */
class FirebaseAuthGateway : AuthGateway {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val auth = Firebase.auth

    override val currentUser: StateFlow<User?> = auth.authStateChanged
        .map { it?.toDomain() }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = auth.currentUser?.toDomain(),
        )

    override suspend fun signIn(email: String, password: String): Result<User> = wrap {
        val result = auth.signInWithEmailAndPassword(email, password)
        checkNotNull(result.user) { "Firebase returned null user after signIn" }.toDomain()
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String?,
    ): Result<User> = wrap {
        val result = auth.createUserWithEmailAndPassword(email, password)
        val user = checkNotNull(result.user) { "Firebase returned null user after register" }
        if (!displayName.isNullOrBlank()) {
            user.updateProfile(displayName = displayName)
        }
        user.toDomain().copy(displayName = displayName)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = wrap {
        auth.sendPasswordResetEmail(email)
    }

    override suspend fun signOut(): Result<Unit> = wrap {
        auth.signOut()
    }

    override suspend fun signInAnonymously(): Result<User> = wrap {
        val result = auth.signInAnonymously()
        checkNotNull(result.user) { "Firebase returned null user after signInAnonymously" }.toDomain()
    }

    // -----------------------------------------------------------------
    // Boundary: nothing above this line leaks Firebase types upward.
    // -----------------------------------------------------------------

    private inline fun <T> wrap(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (t: Throwable) {
        Result.failure(AuthException(mapError(t)))
    }

    private fun FirebaseUser.toDomain(): User = User(
        id = uid,
        email = email,
        displayName = displayName,
        isAnonymous = isAnonymous,
        isEmailVerified = isEmailVerified,
    )

    private fun mapError(throwable: Throwable): AuthError = when (throwable) {
        is FirebaseAuthInvalidCredentialsException -> AuthError.InvalidCredentials
        is FirebaseAuthInvalidUserException -> AuthError.UserNotFound
        is FirebaseAuthUserCollisionException -> AuthError.EmailAlreadyInUse
        is FirebaseAuthWeakPasswordException -> AuthError.WeakPassword
        is FirebaseAuthException -> mapByMessage(throwable.message.orEmpty())
        else -> AuthError.Unknown(cause = throwable.message ?: throwable::class.simpleName.orEmpty())
    }

    /**
     * Fallback for Firebase errors without their own typed exception (rate limiting,
     * network, malformed email). Firebase's error codes are stable strings across platforms.
     */
    private fun mapByMessage(message: String): AuthError {
        val lower = message.lowercase()
        return when {
            "too-many-requests" in lower || "too many" in lower -> AuthError.TooManyRequests
            "network-request-failed" in lower || "network" in lower -> AuthError.NetworkError
            "invalid-email" in lower || "badly formatted" in lower -> AuthError.InvalidEmail
            else -> AuthError.Unknown(cause = message)
        }
    }
}
