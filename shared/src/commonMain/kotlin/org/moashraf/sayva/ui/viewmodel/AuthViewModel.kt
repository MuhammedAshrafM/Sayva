package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.moashraf.sayva.auth.AppBackend
import org.moashraf.sayva.auth.AuthError
import org.moashraf.sayva.auth.AuthException
import org.moashraf.sayva.auth.AuthGateway
import org.moashraf.sayva.auth.User
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway
import org.moashraf.sayva.telemetry.CrashReporter

/**
 * Shared ViewModel across all four auth screens: Login, Register, ForgotPassword,
 * ResetEmailSent. One instance so form state (typed email, etc.) persists as the
 * user moves between screens — a common UX ask ("I typed my email on Login; don't
 * make me type it again on ForgotPassword").
 *
 * ### Error handling
 * `AuthGateway` returns `Result<T>` where failures wrap [AuthException] carrying a
 * typed [AuthError]. We extract that variant into [AuthUiState.error] so screens
 * can pattern-match to show precise UX ("wrong password", "no such user", etc.)
 * instead of a generic message.
 *
 * ### Navigation
 * The ViewModel does not own the NavController. Screens observe [currentUser]
 * and navigate away from the auth flow when it becomes non-null (successful
 * sign-in/register/guest). This keeps navigation decisions in the composable
 * where they belong.
 */
class AuthViewModel(
    private val gateway: AuthGateway,
    private val analytics: AnalyticsGateway,
    private val crashReporter: CrashReporter,
    private val settingsRepository: SettingsRepository,
) {
    // Handler catches unexpected throwables from any of the launched actions
    // and files them as non-fatal crashes — the UI's `state.error` only carries
    // typed AuthErrors, so anything unclassified (network layer bugs, JSON
    // parsing, etc.) would otherwise be swallowed silently.
    private val errorHandler = CoroutineExceptionHandler { _, throwable ->
        crashReporter.recordException(throwable)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default + errorHandler)

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** Observe to know when to navigate away from the auth flow. */
    val currentUser: StateFlow<User?> = gateway.currentUser

    private val backendLabel: String get() = AppBackend.current.name.lowercase()

    fun onEmailChange(value: String) = _state.update {
        it.copy(email = value, error = null)
    }

    fun onPasswordChange(value: String) = _state.update {
        it.copy(password = value, error = null)
    }

    fun onDisplayNameChange(value: String) = _state.update {
        it.copy(displayName = value, error = null)
    }

    fun clearError() = _state.update { it.copy(error = null) }

    /** Reset the "email sent" flag when navigating back to ForgotPassword. */
    fun clearResetSent() = _state.update { it.copy(resetEmailSent = false) }

    fun signIn() = execute(
        attemptEvent = AnalyticsEvents.AUTH_SIGN_IN_ATTEMPTED,
        successEvent = AnalyticsEvents.AUTH_SIGN_IN_SUCCEEDED,
        failureEvent = AnalyticsEvents.AUTH_SIGN_IN_FAILED,
    ) {
        gateway.signIn(state.value.email.trim(), state.value.password)
    }

    fun register() = execute(
        attemptEvent = AnalyticsEvents.AUTH_REGISTER_ATTEMPTED,
        successEvent = AnalyticsEvents.AUTH_REGISTER_SUCCEEDED,
        failureEvent = AnalyticsEvents.AUTH_REGISTER_FAILED,
    ) {
        gateway.register(
            email = state.value.email.trim(),
            password = state.value.password,
            displayName = state.value.displayName.trim().takeIf { it.isNotBlank() },
        )
    }

    fun signInAnonymously() = execute(
        attemptEvent = AnalyticsEvents.AUTH_GUEST_STARTED,
        successEvent = null,
        failureEvent = AnalyticsEvents.AUTH_SIGN_IN_FAILED,
    ) {
        gateway.signInAnonymously()
    }

    fun sendPasswordReset() {
        analytics.logEvent(
            AnalyticsEvents.AUTH_PASSWORD_RESET_REQUESTED,
            mapOf(AnalyticsEvents.Param.BACKEND to backendLabel),
        )
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            gateway.sendPasswordReset(state.value.email.trim())
                .onSuccess {
                    _state.update { it.copy(isLoading = false, resetEmailSent = true) }
                }
                .onFailure { t ->
                    _state.update {
                        it.copy(isLoading = false, error = errorFrom(t))
                    }
                }
        }
    }

    private fun execute(
        attemptEvent: String?,
        successEvent: String?,
        failureEvent: String?,
        block: suspend () -> Result<User>,
    ) {
        if (attemptEvent != null) {
            analytics.logEvent(attemptEvent, mapOf(AnalyticsEvents.Param.BACKEND to backendLabel))
            crashReporter.log("$attemptEvent (backend=$backendLabel)")
        }
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            block()
                .onSuccess {
                    if (successEvent != null) {
                        analytics.logEvent(
                            successEvent,
                            mapOf(AnalyticsEvents.Param.BACKEND to backendLabel),
                        )
                    }
                    // Any successful auth event (sign-in / register / guest)
                    // implies the user has been through the onboarding tour.
                    // Persist so future cold starts route to Login (or Home
                    // if session persists) instead of Welcome — the startup
                    // coordinator reads this flag.
                    settingsRepository.setOnboardingCompleted(true)
                    // currentUser Flow will pick up the new session; screens
                    // observing it will navigate. We just clear the loading flag.
                    _state.update { it.copy(isLoading = false, password = "") }
                }
                .onFailure { t ->
                    val error = errorFrom(t)
                    if (failureEvent != null) {
                        analytics.logEvent(
                            failureEvent,
                            mapOf(
                                AnalyticsEvents.Param.BACKEND to backendLabel,
                                AnalyticsEvents.Param.ERROR to error::class.simpleName.orEmpty(),
                            ),
                        )
                    }
                    _state.update { it.copy(isLoading = false, error = error) }
                }
        }
    }

    private fun errorFrom(t: Throwable): AuthError =
        (t as? AuthException)?.error ?: AuthError.Unknown(t.message ?: t::class.simpleName.orEmpty())
}

/** Form + status state for the auth flow. All four auth screens read from this. */
data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    /** True after `sendPasswordReset` succeeds; the ResetEmailSent screen watches this. */
    val resetEmailSent: Boolean = false,
)

/** User-facing text for an [AuthError] variant. Kept generic-safe for any auth backend. */
fun AuthError.userMessage(): String = when (this) {
    AuthError.InvalidCredentials -> "Wrong email or password."
    AuthError.UserNotFound -> "No account matches that email."
    AuthError.EmailAlreadyInUse -> "An account with that email already exists."
    AuthError.WeakPassword -> "That password is too weak. Use at least 8 characters."
    AuthError.InvalidEmail -> "That email address doesn't look right."
    AuthError.TooManyRequests -> "Too many attempts. Try again in a few minutes."
    AuthError.NetworkError -> "Can't reach the server. Check your connection."
    is AuthError.Unknown -> "Something went wrong. Try again."
}
