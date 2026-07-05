package org.moashraf.sayva.nav

/**
 * Every screen in the app, grouped to match the design's own numbering
 * (Sayva.dc.html sections 01-33). Screens carrying data (e.g. a history
 * item id) hold that data as constructor params; the rest are objects.
 */
sealed class Screen {
    // Onboarding & authentication (01-08)
    data object Welcome : Screen()
    data object HowAiWorks : Screen()
    data object TwoWayIntro : Screen()
    data object Permissions : Screen()
    data object Login : Screen()
    data object Register : Screen()
    data object ForgotPassword : Screen()
    data object ResetEmailSent : Screen()

    // Main app - Home & Translation (09-12)
    data object Home : Screen()
    data object LiveCamera : Screen()
    data object Conversation : Screen()
    data object AiFeedbackLowConfidence : Screen()

    // Memory - History, Favorites, Saved (13-16)
    data object History : Screen()
    data class HistoryDetail(val entryId: String) : Screen()
    data object Favorites : Screen()
    data object SavedConversations : Screen()

    // Learn - the retention engine (17-20)
    data object LearnCategories : Screen()
    data class LessonPlayer(val lessonId: String) : Screen()
    data class Practice(val lessonId: String) : Screen()
    data object Progress : Screen()

    // You - account, settings, accessibility (21-24)
    data object Profile : Screen()
    data object Settings : Screen()
    data object Accessibility : Screen()
    data object Notifications : Screen()

    // Contribute, offline, system states (25-27)
    data object Contribute : Screen()
    data object OfflineModels : Screen()
    data object SystemStates : Screen()

    // Production-critical (28-33)
    data object FirstLaunchModelDownload : Screen()
    data object PairSecondScreen : Screen()
    data object Paywall : Screen()
    data object Family : Screen()
    data object CrashReport : Screen()
    data object InterpreterHandoff : Screen()
}

enum class BottomTab(val screen: Screen) {
    Home(Screen.Home),
    Translate(Screen.LiveCamera),
    Learn(Screen.LearnCategories),
    You(Screen.Profile),
}
