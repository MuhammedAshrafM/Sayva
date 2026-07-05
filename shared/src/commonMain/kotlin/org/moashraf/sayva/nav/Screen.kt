package org.moashraf.sayva.nav

import kotlinx.serialization.Serializable

/**
 * Every screen in the app, grouped to match the design's own numbering
 * (Sayva.dc.html sections 01-33). Screens carrying data (e.g. a history
 * item id) hold that data as constructor params; the rest are objects.
 *
 * ### Why `@Serializable`
 * The nav back-stack is persisted via `rememberSaveable` in
 * [rememberSayvaNavController] so rotation / configuration changes /
 * process death restore the user's current destination instead of
 * bouncing them back to onboarding. The saver serializes each entry to
 * JSON via kotlinx.serialization; this annotation opts every subtype in
 * to polymorphic serialization on the sealed base.
 *
 * If you add a new subclass, annotate it `@Serializable` too — otherwise
 * the app will crash on rotation with a "Serializer for class …" error.
 */
@Serializable
sealed class Screen {
    // Onboarding & authentication (01-08)
    @Serializable data object Welcome : Screen()
    @Serializable data object HowAiWorks : Screen()
    @Serializable data object TwoWayIntro : Screen()
    @Serializable data object Permissions : Screen()
    @Serializable data object Login : Screen()
    @Serializable data object Register : Screen()
    @Serializable data object ForgotPassword : Screen()
    @Serializable data object ResetEmailSent : Screen()

    // Main app - Home & Translation (09-12)
    @Serializable data object Home : Screen()
    @Serializable data object LiveCamera : Screen()
    @Serializable data object Conversation : Screen()
    @Serializable data object AiFeedbackLowConfidence : Screen()

    // Memory - History, Favorites, Saved (13-16)
    @Serializable data object History : Screen()
    @Serializable data class HistoryDetail(val entryId: String) : Screen()
    @Serializable data object Favorites : Screen()
    @Serializable data object SavedConversations : Screen()

    // Learn - the retention engine (17-20)
    @Serializable data object LearnCategories : Screen()
    @Serializable data class LessonPlayer(val lessonId: String) : Screen()
    @Serializable data class Practice(val lessonId: String) : Screen()
    @Serializable data object Progress : Screen()

    // You - account, settings, accessibility (21-24)
    @Serializable data object Profile : Screen()
    @Serializable data object Settings : Screen()
    @Serializable data object Accessibility : Screen()
    @Serializable data object Notifications : Screen()

    // Contribute, offline, system states (25-27)
    @Serializable data object Contribute : Screen()
    @Serializable data object OfflineModels : Screen()
    @Serializable data object SystemStates : Screen()

    // Production-critical (28-33)
    @Serializable data object FirstLaunchModelDownload : Screen()
    @Serializable data object PairSecondScreen : Screen()
    @Serializable data object Paywall : Screen()
    @Serializable data object Family : Screen()
    @Serializable data object CrashReport : Screen()
    @Serializable data object InterpreterHandoff : Screen()
}

enum class BottomTab(val screen: Screen) {
    Home(Screen.Home),
    Translate(Screen.LiveCamera),
    Learn(Screen.LearnCategories),
    You(Screen.Profile),
}
