package org.moashraf.sayva.di

import org.koin.dsl.module
import org.moashraf.sayva.Platform
import org.moashraf.sayva.auth.AppBackend
import org.moashraf.sayva.auth.AuthGateway
import org.moashraf.sayva.auth.BackendFlavor
import org.moashraf.sayva.auth.BiometricPrompt
import org.moashraf.sayva.auth.BiometricPromptProvider
import org.moashraf.sayva.permission.PermissionController
import org.moashraf.sayva.permission.PermissionControllerProvider
import org.moashraf.sayva.data.db.DatabaseDriverFactory
import org.moashraf.sayva.data.firebase.FirebaseAnalyticsGateway
import org.moashraf.sayva.data.firebase.FirebaseAuthGateway
import org.moashraf.sayva.data.firebase.FirebaseCrashReporter
import org.moashraf.sayva.data.repository.ConversationsRepository
import org.moashraf.sayva.data.repository.ConversationsRepositoryImpl
import org.moashraf.sayva.data.repository.FavoritesRepository
import org.moashraf.sayva.data.repository.FavoritesRepositoryImpl
import org.moashraf.sayva.data.repository.HistoryRepository
import org.moashraf.sayva.data.repository.HistoryRepositoryImpl
import org.moashraf.sayva.data.repository.LessonsRepository
import org.moashraf.sayva.data.repository.LessonsRepositoryImpl
import org.moashraf.sayva.data.repository.ProgressRepository
import org.moashraf.sayva.data.repository.ProgressRepositoryImpl
import org.moashraf.sayva.data.repository.SettingsRepository
import org.moashraf.sayva.data.repository.SettingsRepositoryImpl
import org.moashraf.sayva.data.repository.UserRepository
import org.moashraf.sayva.data.repository.UserRepositoryImpl
import org.moashraf.sayva.data.supabase.SupabaseAuthGateway
import org.moashraf.sayva.data.prefs.SettingsStorage
import org.moashraf.sayva.data.prefs.SettingsStorageProvider
import org.moashraf.sayva.data.secure.SecureStorage
import org.moashraf.sayva.data.secure.SecureStorageProvider
import org.moashraf.sayva.db.SayvaDatabase
import org.moashraf.sayva.getPlatform
import org.moashraf.sayva.camera.CameraController
import org.moashraf.sayva.camera.CameraControllerProvider
import org.moashraf.sayva.clipboard.Clipboard
import org.moashraf.sayva.clipboard.ClipboardProvider
import org.moashraf.sayva.languagepack.ComposeResourcePackLoader
import org.moashraf.sayva.languagepack.DefaultLanguagePackRegistry
import org.moashraf.sayva.languagepack.DefaultSignRecognizerFactory
import org.moashraf.sayva.languagepack.DefaultTranslationRenderer
import org.moashraf.sayva.languagepack.LanguagePackController
import org.moashraf.sayva.languagepack.LanguagePackRegistry
import org.moashraf.sayva.languagepack.PackResourceLoader
import org.moashraf.sayva.languagepack.SignRecognizerFactory
import org.moashraf.sayva.languagepack.TranslationRenderer
import org.moashraf.sayva.ml.ArgmaxConfidencePostprocessor
import org.moashraf.sayva.ml.DefaultHandDetectorFactory
import org.moashraf.sayva.ml.HandDetectorFactory
import org.moashraf.sayva.ml.ModelRuntimeRegistry
import org.moashraf.sayva.ml.PostprocessorRegistry
import org.moashraf.sayva.ml.PreprocessorRegistry
import org.moashraf.sayva.ml.adapters.SingleHandKazuhitoPreprocessor
import org.moashraf.sayva.ml.adapters.TfliteRuntimeAdapter
import org.moashraf.sayva.ml.adapters.TwoHandSequencePreprocessor
import org.moashraf.sayva.startup.AppStartupCoordinator
import org.moashraf.sayva.ui.viewmodel.LiveCameraViewModel
import org.moashraf.sayva.telemetry.AnalyticsGateway
import org.moashraf.sayva.telemetry.CrashReporter
import org.moashraf.sayva.ui.viewmodel.AuthViewModel
import org.moashraf.sayva.ui.viewmodel.ConversationsViewModel
import org.moashraf.sayva.ui.viewmodel.FavoritesViewModel
import org.moashraf.sayva.ui.viewmodel.HistoryViewModel
import org.moashraf.sayva.ui.viewmodel.LearnViewModel
import org.moashraf.sayva.ui.viewmodel.ProfileViewModel
import org.moashraf.sayva.ui.viewmodel.SettingsViewModel

/**
 * Root Koin module for the app.
 *
 * The `AuthGateway` binding uses the selector pattern: [AppBackend.current] picks
 * between the Firebase and Supabase adapters. Today only Firebase ships; the
 * `SUPABASE` branch will error until P1-16b lands. Change gradle.properties
 * `sayva.backend=` value to flip the switch — no other code changes.
 *
 * Adds further bindings as later tickets land:
 * - P1-11   → SayvaDatabase (SQLDelight)
 * - P1-12   → SettingsStorage
 * - P1-13   → SecureStorage (expect/actual)
 * - P1-14   → BiometricPrompt (expect/actual)
 * - P1-16b  → SupabaseAuthGateway (fills the empty SUPABASE branch)
 * - P1-20   → Repository interfaces + implementations
 * - P1-21+  → ViewModels
 */
val sayvaModule = module {
    single<Platform> { getPlatform() }

    // ---- Storage layer -----------------------------------------------------
    // Android depends on AndroidAppContext being bootstrapped from MainActivity.
    // Failures during first resolve throw with a clear message pointing to the fix.

    // Encrypted key-value — EncryptedSharedPreferences (Android) / Keychain (iOS)
    single<SecureStorage> { SecureStorageProvider.create() }

    // Non-secret key-value — SharedPreferences (Android) / NSUserDefaults (iOS)
    single<SettingsStorage> { SettingsStorageProvider.create() }

    // SQLDelight-backed relational store
    single { SayvaDatabase(DatabaseDriverFactory.create()) }

    // Auth — selector picks adapter based on BackendFlavor. Change the active
    // backend via `sayva.backend=` in local.properties (rebuild required).
    // Both adapters ship in every build; DI picks one at startup.
    single<AuthGateway> {
        when (AppBackend.current) {
            BackendFlavor.FIREBASE -> FirebaseAuthGateway()
            BackendFlavor.SUPABASE -> SupabaseAuthGateway()
        }
    }

    // Analytics + Crash — Firebase-only for now (Supabase doesn't offer these).
    // Add PostHog/Sentry adapters later if leaving Google's ecosystem.
    single<AnalyticsGateway> { FirebaseAnalyticsGateway() }
    single<CrashReporter> { FirebaseCrashReporter() }

    // ---- Platform capabilities ----------------------------------------------
    // BiometricPrompt requires AndroidActivityProvider on Android (FragmentActivity
    // reference). PermissionController's notifications check on iOS uses
    // UNUserNotificationCenter and can suspend briefly on first call.
    single<BiometricPrompt> { BiometricPromptProvider.create() }
    single<PermissionController> { PermissionControllerProvider.create() }

    // ---- Language Packs -----------------------------------------------------
    // Bundled packs load from Compose Resources (production path). Downloaded
    // packs will be handled by a FileSystemPackResourceLoader added in Phase 4
    // and merged with the bundled ones by DefaultLanguagePackRegistry.
    //
    // The bootstrap sequence (called from App.kt's LaunchedEffect) walks the
    // bundled index, parses every manifest, applies the persisted recognition
    // + output language codes, and emits LanguagePackController.State.Ready.
    // Crashlytics captures the current pack + output as breadcrumb keys once
    // Ready — added when a recognition-consuming screen lands (Phase 2).
    single<PackResourceLoader> { ComposeResourcePackLoader() }
    single<LanguagePackRegistry> {
        DefaultLanguagePackRegistry(
            loader = get(),
            onLoadError = { code, cause ->
                get<CrashReporter>().recordException(
                    IllegalStateException("LanguagePack '$code' failed to load", cause),
                )
            },
        )
    }
    single<TranslationRenderer> { DefaultTranslationRenderer() }

    // ---- ML adapter registries ---------------------------------------------
    // Each pack model manifest names four adapter IDs (runtime, preprocessing,
    // postprocessing, inferenceStrategy). Registries below hold the concrete
    // adapters this app version ships. A pack referring to an unknown ID
    // fails at recognizer construction with a clear "requires app v N.M+"
    // error — no silent fallback, no per-pack code branches.
    //
    // Adding an adapter for a future pack is one commit:
    //   1. Implement the port (ModelRuntimeFactory / Preprocessor / Postprocessor)
    //   2. Add the entry to the map inside the appropriate `single` below
    //   3. Bump the app version + the pack's minAppVersion
    //
    // ### Why maps are inlined into the registry constructors
    // Previously each map was its own `single<Map<String, X>> { ... }` binding.
    // Kotlin JVM generic erasure meant all three collapsed to `Map` at Koin
    // resolution time, and every registry received the last-registered map
    // (the postprocessors). The recognizer would throw at first frame with
    // "Available: [argmax_confidence_v1]" for a runtime lookup — a real bug
    // caught during on-device testing. Inlining removes the ambiguity.
    single {
        ModelRuntimeRegistry(
            mapOf(
                TfliteRuntimeAdapter.ID to TfliteRuntimeAdapter,
            )
        )
    }
    single {
        PreprocessorRegistry(
            mapOf(
                SingleHandKazuhitoPreprocessor.ID to SingleHandKazuhitoPreprocessor,
                TwoHandSequencePreprocessor.ID to TwoHandSequencePreprocessor,
            )
        )
    }
    single {
        PostprocessorRegistry(
            mapOf(
                ArgmaxConfidencePostprocessor.ID to ArgmaxConfidencePostprocessor,
            )
        )
    }

    single<SignRecognizerFactory> {
        DefaultSignRecognizerFactory(
            loader = get(),
            runtimeRegistry = get(),
            preprocessorRegistry = get(),
            postprocessorRegistry = get(),
        )
    }
    single {
        LanguagePackController(
            registry = get(),
            settings = get(),
            loader = get(),
            crashReporter = get(),
        )
    }

    // ---- Camera + Hand Detection --------------------------------------------
    // Both are per-platform. CameraControllerProvider / HandDetectorProvider
    // wrap the concrete Android (CameraX + MediaPipe) or iOS (AVCaptureSession
    // + MediaPipe iOS, stubbed until Mac session) implementations. The
    // pipeline receives factories, not the singletons, so tests can stub.
    single<CameraController> { CameraControllerProvider.create() }
    single<HandDetectorFactory> { DefaultHandDetectorFactory() }

    // Platform clipboard bridge — ClipboardManager on Android, UIPasteboard
    // on iOS. UI never imports the platform types.
    single<Clipboard> { ClipboardProvider.create() }

    // ---- Repositories -------------------------------------------------------
    // ViewModels depend on interfaces only. All 7 repo impls now shipped.
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get(), get(), get(), get()) }
    single<HistoryRepository> { HistoryRepositoryImpl(get()) }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get()) }
    single<ConversationsRepository> { ConversationsRepositoryImpl(get()) }
    single<ProgressRepository> { ProgressRepositoryImpl(get()) }
    single<LessonsRepository> { LessonsRepositoryImpl(get()) }

    // ---- ViewModels ---------------------------------------------------------
    // Registered as `single` (not `viewModel { ... }` DSL) because the DSL's
    // import path varies across Koin versions on KMP. Screens use `koinInject()`.
    // Practical difference is small: a `single` ViewModel lives for the whole
    // app process, which is fine for a Settings VM that mirrors a repo state.
    // If we later add feature-scoped VMs that hold navigation-back-stack state,
    // switch to `factory { ... }` + `koinViewModel()` at that point.
    single { SettingsViewModel(get(), get(), get()) }
    single { HistoryViewModel(get(), get()) }
    single { FavoritesViewModel(get(), get()) }
    single { ProfileViewModel(get(), get()) }
    single { ConversationsViewModel(get(), get()) }
    single { LearnViewModel(get(), get(), get()) }
    // Auth VM is shared across Login/Register/ForgotPassword/ResetEmailSent so
    // form state (typed email) persists across screen transitions in the flow.
    single { AuthViewModel(get(), get(), get(), get()) }

    // ---- Startup coordinator ------------------------------------------------
    // Resolves the app's initial destination from persisted auth + onboarding
    // state on cold start. App.kt renders a splash while Resolving, then
    // constructs the nav controller with the resolved destination.
    single { AppStartupCoordinator(authGateway = get(), settingsRepository = get()) }

    // Live camera / recognition — owns the pipeline lifecycle.
    single {
        LiveCameraViewModel(
            camera = get(),
            handDetectorFactory = get(),
            signRecognizerFactory = get(),
            translationRenderer = get(),
            packController = get(),
            permissionController = get(),
            analytics = get(),
            crashReporter = get(),
            favorites = get(),
            settings = get(),
            clipboard = get(),
        )
    }
}
