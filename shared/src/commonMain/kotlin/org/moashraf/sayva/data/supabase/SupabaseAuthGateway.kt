package org.moashraf.sayva.data.supabase

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
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
import org.moashraf.sayva.buildkonfig.BuildKonfig

/**
 * Supabase implementation of [AuthGateway]. **Only file** in the codebase that
 * imports `io.github.jan.supabase.*` types — the rest of the app talks to the
 * [AuthGateway] interface.
 *
 * ### Configuration
 * `SUPABASE_URL` and `SUPABASE_ANON_KEY` come from `local.properties` via
 * BuildKonfig. If they're blank, [SupabaseAuthGateway] refuses to initialize
 * with a clear message rather than failing with a confusing HTTP error later.
 *
 * ### Session persistence
 * `Auth.install()` installs the default session manager which persists to
 * platform-appropriate storage (SharedPreferences on Android via multiplatform-settings,
 * Keychain on iOS). This is separate from our own [org.moashraf.sayva.data.secure.SecureStorage]
 * — Supabase manages its own tokens because it knows how to refresh them.
 *
 * ### Error mapping
 * Supabase's error codes are HTTP-flavored ("invalid_credentials", "email_exists")
 * rather than typed exceptions. We inspect exception code + message to map to
 * [AuthError] variants. This matches Firebase's mapping so identical failures
 * produce identical `AuthError` values regardless of active backend.
 */
class SupabaseAuthGateway : AuthGateway {

    init {
        check(BuildKonfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL is blank. Add sayva.supabase.url=... to local.properties."
        }
        check(BuildKonfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_ANON_KEY is blank. Add sayva.supabase.anonKey=... to local.properties."
        }
    }

    private val client = createSupabaseClient(
        supabaseUrl = BuildKonfig.SUPABASE_URL,
        supabaseKey = BuildKonfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth)
    }

    private val supabaseAuth = client.auth
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val currentUser: StateFlow<User?> = supabaseAuth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user?.toDomain()
                else -> null
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = supabaseAuth.currentUserOrNull()?.toDomain(),
        )

    override suspend fun signIn(email: String, password: String): Result<User> = wrap {
        supabaseAuth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        supabaseAuth.currentUserOrNull()?.toDomain()
            ?: throw IllegalStateException("Supabase returned null user after signIn")
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String?,
    ): Result<User> = wrap {
        supabaseAuth.signUpWith(Email) {
            this.email = email
            this.password = password
            if (!displayName.isNullOrBlank()) {
                data = kotlinx.serialization.json.buildJsonObject {
                    put("display_name", kotlinx.serialization.json.JsonPrimitive(displayName))
                }
            }
        }
        supabaseAuth.currentUserOrNull()?.toDomain()?.copy(displayName = displayName)
            ?: throw IllegalStateException("Supabase returned null user after register")
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = wrap {
        supabaseAuth.resetPasswordForEmail(email)
    }

    override suspend fun signOut(): Result<Unit> = wrap {
        supabaseAuth.signOut()
    }

    override suspend fun signInAnonymously(): Result<User> = wrap {
        supabaseAuth.signInAnonymously()
        supabaseAuth.currentUserOrNull()?.toDomain()
            ?: throw IllegalStateException("Supabase returned null user after signInAnonymously")
    }

    // -----------------------------------------------------------------
    // Boundary: nothing above this line leaks Supabase types upward.
    // -----------------------------------------------------------------

    private inline fun <T> wrap(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (t: Throwable) {
        Result.failure(AuthException(mapError(t)))
    }

    private fun UserInfo.toDomain(): User {
        // Supabase doesn't expose an isAnonymous flag on UserInfo directly (unlike
        // Firebase). Anonymous users are identified by their `appMetadata.provider`
        // field being "anonymous" — that's set by `auth.signInAnonymously()`.
        val provider = appMetadata?.get("provider")?.toString()?.trim('"')
        return User(
            id = id,
            email = email,
            displayName = userMetadata?.get("display_name")?.toString()?.trim('"'),
            isAnonymous = provider == "anonymous",
            isEmailVerified = emailConfirmedAt != null,
        )
    }

    /**
     * Map Supabase exceptions to normalized [AuthError] variants.
     *
     * Supabase's error taxonomy is HTTP-flavored: exceptions carry a `statusCode`
     * and an error code string. We inspect both to match the shape of the failure
     * to the interface's error variants.
     */
    private fun mapError(throwable: Throwable): AuthError = when (throwable) {
        is HttpRequestException -> AuthError.NetworkError
        is AuthRestException -> mapAuthRestException(throwable)
        is RestException -> mapRestException(throwable)
        else -> AuthError.Unknown(cause = throwable.message ?: throwable::class.simpleName.orEmpty())
    }

    private fun mapAuthRestException(e: AuthRestException): AuthError {
        val code = e.errorCode?.value?.lowercase().orEmpty()
        val msg = e.message.orEmpty().lowercase()
        return when {
            "invalid_grant" in code || "invalid_credentials" in code ||
                "invalid login credentials" in msg -> AuthError.InvalidCredentials

            "user_not_found" in code || "user not found" in msg -> AuthError.UserNotFound

            "email_exists" in code || "user_already_exists" in code ||
                "already registered" in msg -> AuthError.EmailAlreadyInUse

            "weak_password" in code || "password" in msg && "short" in msg ->
                AuthError.WeakPassword

            "invalid_email" in code || "email" in msg && "invalid" in msg ->
                AuthError.InvalidEmail

            "over_email_send_rate_limit" in code || "rate" in msg ->
                AuthError.TooManyRequests

            else -> AuthError.Unknown(cause = "supabase auth: ${e.message}")
        }
    }

    private fun mapRestException(e: RestException): AuthError = when (e.statusCode) {
        429 -> AuthError.TooManyRequests
        in 500..599 -> AuthError.NetworkError
        else -> AuthError.Unknown(cause = "supabase ${e.statusCode}: ${e.message}")
    }
}
