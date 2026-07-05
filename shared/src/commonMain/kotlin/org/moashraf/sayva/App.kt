package org.moashraf.sayva

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.moashraf.sayva.designsystem.SayvaTheme
import org.moashraf.sayva.di.sayvaModule
import org.moashraf.sayva.languagepack.LanguagePackController
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.nav.rememberSayvaNavController
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway
import org.moashraf.sayva.telemetry.CrashReporter
import org.moashraf.sayva.ui.components.SayvaBottomNav
import org.moashraf.sayva.ui.screens.critical.CrashReportScreen
import org.moashraf.sayva.ui.screens.critical.FamilyScreen
import org.moashraf.sayva.ui.screens.critical.FirstLaunchModelDownloadScreen
import org.moashraf.sayva.ui.screens.critical.InterpreterHandoffScreen
import org.moashraf.sayva.ui.screens.critical.PairSecondScreenContent
import org.moashraf.sayva.ui.screens.critical.PaywallScreen
import org.moashraf.sayva.ui.screens.home.AiFeedbackLowConfidenceScreen
import org.moashraf.sayva.ui.screens.home.ConversationScreen
import org.moashraf.sayva.ui.screens.home.HomeScreen
import org.moashraf.sayva.ui.screens.home.LiveCameraScreen
import org.moashraf.sayva.ui.screens.learn.LearnCategoriesScreen
import org.moashraf.sayva.ui.screens.learn.LessonPlayerScreen
import org.moashraf.sayva.ui.screens.learn.PracticeScreen
import org.moashraf.sayva.ui.screens.learn.ProgressScreen
import org.moashraf.sayva.ui.screens.memory.FavoritesScreen
import org.moashraf.sayva.ui.screens.memory.HistoryDetailScreen
import org.moashraf.sayva.ui.screens.memory.HistoryScreen
import org.moashraf.sayva.ui.screens.memory.SavedConversationsScreen
import org.moashraf.sayva.ui.screens.onboarding.ForgotPasswordScreen
import org.moashraf.sayva.ui.screens.onboarding.HowAiWorksScreen
import org.moashraf.sayva.ui.screens.onboarding.LoginScreen
import org.moashraf.sayva.ui.screens.onboarding.PermissionsScreen
import org.moashraf.sayva.ui.screens.onboarding.RegisterScreen
import org.moashraf.sayva.ui.screens.onboarding.ResetEmailSentScreen
import org.moashraf.sayva.ui.screens.onboarding.TwoWayIntroScreen
import org.moashraf.sayva.ui.screens.onboarding.WelcomeScreen
import org.moashraf.sayva.ui.screens.system.ContributeScreen
import org.moashraf.sayva.ui.screens.system.OfflineModelsScreen
import org.moashraf.sayva.ui.screens.system.SystemStatesScreen
import org.moashraf.sayva.ui.screens.you.AccessibilityScreen
import org.moashraf.sayva.ui.screens.you.NotificationsScreen
import org.moashraf.sayva.ui.screens.you.ProfileScreen
import org.moashraf.sayva.ui.screens.you.SettingsScreen

/**
 * Snake-case analytics name for a screen. Uses [Screen] simpleName so adding a
 * new screen automatically gets a sensible name — override here if the sealed
 * class name doesn't map cleanly to what we want to see in reports.
 */
private fun Screen.analyticsName(): String {
    val raw = this::class.simpleName ?: "unknown"
    // "LiveCamera" -> "live_camera". CamelCase → snake_case.
    return buildString(raw.length + 4) {
        raw.forEachIndexed { i, c ->
            if (i > 0 && c.isUpperCase()) append('_')
            append(c.lowercaseChar())
        }
    }
}

private val bottomNavRoots = setOf(
    Screen.Home::class,
    Screen.LiveCamera::class,
    Screen.LearnCategories::class,
    Screen.Profile::class,
)

@Composable
fun App() {
    KoinApplication(application = { modules(sayvaModule) }) {
        SayvaTheme {
            val nav = rememberSayvaNavController()
            val current = nav.current

            // Language Pack bootstrap — fires once at app start. Populates
            // the pack registry from Compose Resources and applies the
            // persisted (or MVP-default) recognition + output languages.
            // Screens that read `LanguagePackController.state` show a
            // loading placeholder until this completes; everything else
            // (auth, favorites, history, settings, learn) works uninterrupted.
            val packController: LanguagePackController = koinInject()
            LaunchedEffect(Unit) {
                packController.bootstrap()
            }

            // Screen-view + breadcrumb tracking — fires on every navigation.
            // Placed here so no per-screen wiring is required and the analytics
            // hit and Crashlytics breadcrumb use identical names.
            val analytics: AnalyticsGateway = koinInject()
            val crashReporter: CrashReporter = koinInject()
            LaunchedEffect(current) {
                val name = current.analyticsName()
                analytics.logScreenView(name)
                analytics.logEvent(
                    AnalyticsEvents.SCREEN_VIEWED,
                    mapOf(AnalyticsEvents.Param.SCREEN_NAME to name),
                )
                crashReporter.setKey("current_screen", name)
                crashReporter.log("nav → $name")
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        RenderScreen(nav, current)
                    }
                    if (current::class in bottomNavRoots) {
                        SayvaBottomNav(
                            current = current,
                            onSelect = { tab -> nav.replaceAll(tab.screen) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderScreen(nav: SayvaNavController, screen: Screen) {
    when (screen) {
        Screen.Welcome -> WelcomeScreen(nav)
        Screen.HowAiWorks -> HowAiWorksScreen(nav)
        Screen.TwoWayIntro -> TwoWayIntroScreen(nav)
        Screen.Permissions -> PermissionsScreen(nav)
        Screen.Login -> LoginScreen(nav)
        Screen.Register -> RegisterScreen(nav)
        Screen.ForgotPassword -> ForgotPasswordScreen(nav)
        Screen.ResetEmailSent -> ResetEmailSentScreen(nav)

        Screen.Home -> HomeScreen(nav)
        Screen.LiveCamera -> LiveCameraScreen(nav)
        Screen.Conversation -> ConversationScreen(nav)
        Screen.AiFeedbackLowConfidence -> AiFeedbackLowConfidenceScreen(nav)

        Screen.History -> HistoryScreen(nav)
        is Screen.HistoryDetail -> HistoryDetailScreen(nav, screen.entryId)
        Screen.Favorites -> FavoritesScreen(nav)
        Screen.SavedConversations -> SavedConversationsScreen(nav)

        Screen.LearnCategories -> LearnCategoriesScreen(nav)
        is Screen.LessonPlayer -> LessonPlayerScreen(nav, screen.lessonId)
        is Screen.Practice -> PracticeScreen(nav, screen.lessonId)
        Screen.Progress -> ProgressScreen(nav)

        Screen.Profile -> ProfileScreen(nav)
        Screen.Settings -> SettingsScreen(nav)
        Screen.Accessibility -> AccessibilityScreen(nav)
        Screen.Notifications -> NotificationsScreen(nav)

        Screen.Contribute -> ContributeScreen(nav)
        Screen.OfflineModels -> OfflineModelsScreen(nav)
        Screen.SystemStates -> SystemStatesScreen(nav)

        Screen.FirstLaunchModelDownload -> FirstLaunchModelDownloadScreen(nav)
        Screen.PairSecondScreen -> PairSecondScreenContent(nav)
        Screen.Paywall -> PaywallScreen(nav)
        Screen.Family -> FamilyScreen(nav)
        Screen.CrashReport -> CrashReportScreen(nav)
        Screen.InterpreterHandoff -> InterpreterHandoffScreen(nav)
    }
}
