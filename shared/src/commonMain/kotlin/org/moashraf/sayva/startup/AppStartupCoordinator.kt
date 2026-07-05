package org.moashraf.sayva.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.moashraf.sayva.auth.AuthGateway
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.nav.Screen

/**
 * Resolves the app's initial navigation destination on cold start.
 *
 * Fixes the bug where every launch dropped the user on `Welcome` — even if
 * they had a persisted authenticated session. The coordinator observes
 * [AuthGateway.currentUser] + [SettingsRepository.state.onboardingCompleted]
 * and emits one of three [Destination]s:
 *
 *   * [Destination.Home] — user has a persisted signed-in session (Firebase
 *     Auth restores this synchronously from disk on first read)
 *   * [Destination.Login] — user completed onboarding previously but is
 *     signed out. They know the app; drop straight into the auth flow
 *     without walking the Welcome/HowAiWorks/TwoWayIntro/Permissions tour
 *     again.
 *   * [Destination.Welcome] — genuine first-run. Full onboarding.
 *
 * ### State shape
 * [state] starts in [StartupState.Resolving]. The App shell renders a
 * splash placeholder in that state so we don't flash `Welcome` before
 * transitioning to `Home`. It transitions once to [StartupState.Ready]
 * and stays there — this is not a running observer of auth state, just
 * a one-shot resolution at app launch.
 *
 * ### Sign-out behavior
 * After sign-out, `AuthGateway.currentUser` emits `null` but this
 * coordinator does NOT re-resolve — the ProfileScreen's sign-out action
 * calls `nav.replaceAll(Screen.Login)` directly (that's a UI concern,
 * not a startup concern).
 */
class AppStartupCoordinator(
    private val authGateway: AuthGateway,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = MainScope(),
) {

    sealed class StartupState {
        data object Resolving : StartupState()
        data class Ready(val initialDestination: Screen) : StartupState()
    }

    private val _state = MutableStateFlow<StartupState>(StartupState.Resolving)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    /**
     * Kick off the resolution. Called once from `App.kt`'s `LaunchedEffect`
     * at cold start. Idempotent — repeated calls after Ready are no-ops.
     */
    fun resolve() {
        if (_state.value is StartupState.Ready) return
        scope.launch {
            _state.value = StartupState.Ready(resolveDestination())
        }
    }

    private suspend fun resolveDestination(): Screen = withContext(Dispatchers.Default) {
        // Wait for the auth state's first emission — Firebase Auth publishes
        // the persisted user synchronously so this returns near-instantly on
        // real installs. `first()` guards against any adapter that has a
        // brief "resolving" gap before emitting.
        val user = authGateway.currentUser.first()
        val onboardingDone = settingsRepository.state.value.onboardingCompleted
        when {
            user != null -> Screen.Home
            onboardingDone -> Screen.Login
            else -> Screen.Welcome
        }
    }
}
