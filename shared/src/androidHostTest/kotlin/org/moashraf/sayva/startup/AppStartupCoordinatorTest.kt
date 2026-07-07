package org.moashraf.sayva.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.moashraf.sayva.auth.AuthError
import org.moashraf.sayva.auth.AuthGateway
import org.moashraf.sayva.auth.User
import org.moashraf.sayva.data.repository.CameraQuality
import org.moashraf.sayva.data.repository.ColorBlindMode
import org.moashraf.sayva.data.repository.DisplayMode
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.data.repository.SettingsState
import org.moashraf.sayva.nav.Screen
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Regression guard for a real bug caught during on-device testing: every cold
 * start landed on `Welcome` even when the user had a persisted authenticated
 * session. The coordinator resolves the initial destination from
 * [AuthGateway.currentUser] + [SettingsRepository.state.onboardingCompleted].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStartupCoordinatorTest {

    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        kotlinx.coroutines.Dispatchers.setMain(UnconfinedTestDispatcher())
        scope = CoroutineScope(Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun `authenticated user routes to Home`() = runBlocking<Unit> {
        val auth = FakeAuthGateway(user = fakeUser())
        val settings = StartupFakeSettings()
        val coordinator = AppStartupCoordinator(auth, settings, scope)

        coordinator.resolve()
        val ready = coordinator.awaitReady()

        assertEquals(Screen.Home, ready.initialDestination)
    }

    @Test
    fun `unauthenticated user with onboarding completed routes to Login`() = runBlocking<Unit> {
        val auth = FakeAuthGateway(user = null)
        val settings = StartupFakeSettings(SettingsState(onboardingCompleted = true))
        val coordinator = AppStartupCoordinator(auth, settings, scope)

        coordinator.resolve()
        val ready = coordinator.awaitReady()

        assertEquals(Screen.Login, ready.initialDestination)
    }

    @Test
    fun `first-run unauthenticated user routes to Welcome`() = runBlocking<Unit> {
        val auth = FakeAuthGateway(user = null)
        val settings = StartupFakeSettings(SettingsState(onboardingCompleted = false))
        val coordinator = AppStartupCoordinator(auth, settings, scope)

        coordinator.resolve()
        val ready = coordinator.awaitReady()

        assertEquals(Screen.Welcome, ready.initialDestination)
    }

    @Test
    fun `state starts in Resolving before resolve is called`() = runBlocking<Unit> {
        val coordinator = AppStartupCoordinator(
            FakeAuthGateway(user = null),
            StartupFakeSettings(),
            scope,
        )
        assertIs<AppStartupCoordinator.StartupState.Resolving>(coordinator.state.value)
    }

    @Test
    fun `resolve is idempotent after Ready`() = runBlocking<Unit> {
        val auth = FakeAuthGateway(user = fakeUser())
        val settings = StartupFakeSettings()
        val coordinator = AppStartupCoordinator(auth, settings, scope)

        coordinator.resolve()
        val firstReady = coordinator.awaitReady()

        // Second call — auth state unchanged, so re-resolve is a no-op
        coordinator.resolve()
        val secondReady = coordinator.state.value

        assertEquals(firstReady, secondReady)
    }

    private suspend fun AppStartupCoordinator.awaitReady(
        timeoutMillis: Long = 2_000,
    ): AppStartupCoordinator.StartupState.Ready = withTimeout(timeoutMillis) {
        state.filter { it is AppStartupCoordinator.StartupState.Ready }
            .first() as AppStartupCoordinator.StartupState.Ready
    }

    private fun fakeUser(): User = User(
        id = "u1",
        email = "test@example.com",
        displayName = "Test",
        isAnonymous = false,
        isEmailVerified = true,
    )
}

private class FakeAuthGateway(
    user: User?,
) : AuthGateway {
    private val _current = MutableStateFlow(user)
    override val currentUser: StateFlow<User?> = _current.asStateFlow()

    override suspend fun signIn(email: String, password: String): Result<User> = error("unused")
    override suspend fun register(email: String, password: String, displayName: String?): Result<User> = error("unused")
    override suspend fun sendPasswordReset(email: String): Result<Unit> = error("unused")
    override suspend fun signOut(): Result<Unit> = error("unused")
    override suspend fun signInAnonymously(): Result<User> = error("unused")

    @Suppress("unused") private val _errorTypeCheck: AuthError = AuthError.Unknown("")
}

private class StartupFakeSettings(initial: SettingsState = SettingsState()) : SettingsRepository {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<SettingsState> = _state.asStateFlow()

    override fun setOnboardingCompleted(completed: Boolean) {
        _state.value = _state.value.copy(onboardingCompleted = completed)
    }

    override fun setRecognitionLanguageCode(code: String?) {}
    override fun setOutputLanguageCode(code: String?) {}
    override fun setDisplayMode(mode: DisplayMode) {}
    override fun setFontSizeScale(scale: Float) {}
    override fun setHighContrast(enabled: Boolean) {}
    override fun setVoiceSpeed(speed: Float) {}
    override fun setCameraQuality(quality: CameraQuality) {}
    override fun setOfflineMode(enabled: Boolean) {}
    override fun setEmergencyMode(enabled: Boolean) {}
    override fun setEasyMode(enabled: Boolean) {}
    override fun setLargerText(enabled: Boolean) {}
    override fun setColorBlindMode(mode: ColorBlindMode) {}
    override fun setLeftHandedMode(enabled: Boolean) {}
    override fun setHapticIntensity(intensity: Float) {}
    override fun setReduceMotion(enabled: Boolean) {}
    override fun setScreenReaderHints(enabled: Boolean) {}
    override fun setDeveloperMode(enabled: Boolean) {}
    override fun resetToDefaults() { _state.value = SettingsState() }
}
