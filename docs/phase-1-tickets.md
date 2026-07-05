# Phase 1 — Ticket Breakdown

> **Parent plan:** `C:\Users\amirm\.claude\plans\harmonic-coalescing-pond.md` — refer to Phase 1 sections § 1.1 through § 1.7 for full context and reasoning.

## How to Use This Document

Each ticket is intended to be a single reviewable PR. Tickets are grouped into **4 waves** by dependency depth — Wave 1 has no prerequisites and can start immediately; each later wave depends on the wave above being green.

Within a wave, tickets can run in parallel across engineers. Between waves, respect the dependency chain.

**Effort key:** `S` < 1 day · `M` 1–3 days · `L` 3–5 days. Nothing over `L`; anything bigger has been split.

**Acceptance criteria** are what a reviewer verifies before merge. If a ticket ships without meeting them, it's not done.

---

## Table of Contents

- [Sequencing Overview](#sequencing-overview)
- [Ticket Index](#ticket-index)
- [Wave 1: Kickoff — No Dependencies](#wave-1-kickoff--no-dependencies)
- [Wave 2: Core Infrastructure](#wave-2-core-infrastructure)
- [Wave 3: Feature Integration](#wave-3-feature-integration)
- [Wave 4: Verification](#wave-4-verification)
- [External / Non-Code Work](#external--non-code-work)
- [Risks & Watchouts](#risks--watchouts)

---

## Sequencing Overview

```
Wave 1  ──▶  Wave 2  ──▶  Wave 3  ──▶  Wave 4
kickoff     core infra   integration    verify
~1 week     ~2 weeks     ~2-3 weeks    ~1 week
```

| Wave | Purpose | Blocks | Parallelizable |
|---|---|---|---|
| 1 | Dependencies added, scaffolds in place, no behavior change yet | Everything downstream | Yes — all Wave 1 tickets are independent |
| 2 | Repositories, database, DI, expect/actual pairs — plumbing works but no UI touches this yet | Wave 3 | Mostly — some interdependencies noted per ticket |
| 3 | Every screen migrates off `MockSayvaData` onto ViewModels + repositories. Semantics added. | Wave 4 | By feature area — each engineer takes one area |
| 4 | End-to-end verification of Phase 1 exit criteria | — | Serial — needs everything else done |

---

## Ticket Index

| ID | Title | Wave | § | Depends on | Effort |
|---|---|---|---|---|---|
| P1-01 | Add Koin KMP dependency | 1 | 1.1 | — | S |
| P1-02 | Add SQLDelight plugin and dependency | 1 | 1.2 | — | S |
| P1-03 | Add multiplatform-settings dependency | 1 | 1.2 | — | S |
| P1-04a | Define gateway interfaces + BackendFlavor enum | 1 | 1.3, 1.6 | — | S |
| P1-04b | Add Firebase KMP dependencies + config files | 1 | 1.3, 1.6 | P1-04a | M |
| P1-04c | Add Supabase KMP dependencies + config (BuildKonfig) | 1 | 1.3 | P1-04a | M |
| P1-05 | Declare permissions in AndroidManifest + Info.plist | 1 | 1.4 | — | S |
| P1-06 | Add `contentDescription` parameter to `SymbolIcon` | 1 | 1.5 | — | S |
| P1-07 | Scaffold `ml/` Python subproject | 1 | 1.7 | — | M |
| P1-08 | CI workflow split (app-ci + ml-ci with path filters) | 1 | 1.7 | P1-07 | S |
| P1-09 | Install Koin in `App.kt` with a hello-world service | 2 | 1.1 | P1-01 | S |
| P1-10 | SQLDelight schema files for all mock models | 2 | 1.2 | P1-02 | M |
| P1-11 | Database driver expect/actual | 2 | 1.2 | P1-02 | M |
| P1-12 | Settings storage expect/actual (multiplatform-settings) | 2 | 1.2 | P1-03 | S |
| P1-13 | Secure storage expect/actual (Keychain / EncryptedSharedPreferences) | 2 | 1.3 | — | M |
| P1-14 | Biometric prompt expect/actual | 2 | 1.3 | — | M |
| P1-15 | Permission controller expect/actual | 2 | 1.4 | P1-05 | M |
| P1-16 | Firebase adapters (Auth + Analytics + Crash) implementing gateway interfaces | 2 | 1.3, 1.6 | P1-04a, P1-04b | M |
| P1-16b | Supabase Auth adapter (SupabaseAuthGateway) + Koin selector | 2 | 1.3 | P1-04a, P1-04c, P1-16 | M |
| P1-17 | Vocabulary codegen pipeline (YAML → `Vocabulary.kt`) | 2 | 1.7, 2.3 | P1-07 | M |
| P1-18 | Toy 5-sign training loop in `ml/` | 2 | 1.7 | P1-07 | L |
| P1-19 | TFLite + CoreML export prototype in `ml/` | 2 | 1.7 | P1-18 | L |
| P1-20 | Repository interfaces (all 7) in `commonMain` | 3 | 1.1 | P1-09 | M |
| P1-21 | SettingsRepository + ViewModel + wire `SettingsScreen` + `AccessibilityScreen` | 3 | 1.1, 1.2 | P1-12, P1-20 | M |
| P1-22 | HistoryRepository + ViewModel + wire `HistoryScreen` + `HistoryDetailScreen` | 3 | 1.1, 1.2 | P1-10, P1-11, P1-20 | M |
| P1-23 | FavoritesRepository + ViewModel + wire `FavoritesScreen` | 3 | 1.1, 1.2 | P1-10, P1-11, P1-20 | M |
| P1-24 | ConversationsRepository + ViewModel + wire `ConversationScreen` + `SavedConversationsScreen` | 3 | 1.1, 1.2 | P1-10, P1-11, P1-20 | M |
| P1-25 | LessonsRepository + ProgressRepository + wire 4 Learn screens | 3 | 1.1, 1.2 | P1-10, P1-11, P1-20 | L |
| P1-26 | AuthRepository (wraps AuthGateway) + UserRepository + wire 4 auth screens | 3 | 1.1, 1.3 | P1-04a, P1-13, P1-14, P1-16b, P1-20 | L |
| P1-27 | Wire real permission requests into `PermissionsScreen` | 3 | 1.4 | P1-15 | S |
| P1-28 | Wire Analytics events + Crashlytics into ViewModels | 3 | 1.6 | P1-16, P1-20 | M |
| P1-29 | Wire `CrashReportScreen` to real crash log submission | 3 | 1.6 | P1-16 | S |
| P1-30 | Add semantics to shared components (`Bars`, `Buttons`, `Misc`) | 3 | 1.5 | P1-06 | S |
| P1-31 | Add semantics to Onboarding screens (8 screens) | 3 | 1.5 | P1-06, P1-30 | M |
| P1-32 | Add semantics to Home/Translation screens (4 screens) | 3 | 1.5 | P1-06, P1-30 | M |
| P1-33 | Add semantics to Memory screens (4 screens) | 3 | 1.5 | P1-06, P1-30 | M |
| P1-34 | Add semantics to Learn screens (4 screens) | 3 | 1.5 | P1-06, P1-30 | M |
| P1-35 | Add semantics to You screens (4 screens) | 3 | 1.5 | P1-06, P1-30 | M |
| P1-36 | Add semantics to System + Critical screens (10 screens) | 3 | 1.5 | P1-06, P1-30 | L |
| P1-37 | Phase 1 verification walkthrough | 4 | all | all Wave 3 | M |

**Total:** 37 tickets · Roughly 6 weeks of work for 2 engineers running in parallel.

---

## Wave 1: Kickoff — No Dependencies

### P1-01: Add Koin KMP dependency
**Section:** § 1.1 · **Effort:** S · **Depends on:** —

Add `io.insert-koin:koin-core` and `io.insert-koin:koin-compose` to the version catalog and shared module. No code that uses Koin yet — this ticket is purely dependency addition.

**Files:**
- Modify: `gradle/libs.versions.toml` — add `koin = "3.x.x"` and dependency entries
- Modify: `shared/build.gradle.kts` — add Koin to `commonMain.dependencies`

**Acceptance:**
- `./gradlew :shared:assemble` succeeds
- Both iOS targets still compile
- No behavior change

---

### P1-02: Add SQLDelight plugin and dependency
**Section:** § 1.2 · **Effort:** S · **Depends on:** —

Add SQLDelight Gradle plugin and runtime dependency. No schemas defined yet.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts` — apply plugin, configure `sqldelight { databases { create("SayvaDatabase") { ... } } }`

**Acceptance:**
- Build succeeds
- SQLDelight source set directory exists at `shared/src/commonMain/sqldelight/`

---

### P1-03: Add multiplatform-settings dependency
**Section:** § 1.2 · **Effort:** S · **Depends on:** —

Add `com.russhwolf:multiplatform-settings` for KMP-shared preferences storage.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`

**Acceptance:**
- Build succeeds on both platforms

---

### P1-04a: Define gateway interfaces + BackendFlavor enum
**Section:** § 1.3, § 1.6 · **Effort:** S · **Depends on:** —

Define provider-agnostic interfaces, domain models, and the backend selector. **Zero vendor SDK imports.** The interfaces must satisfy BOTH Firebase and Supabase — do the cross-check before merging.

**Files (all new, all provider-agnostic):**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/auth/AuthGateway.kt`
  - `interface AuthGateway { suspend fun signIn(email, password): Result<User, AuthError>; suspend fun register(...); suspend fun sendPasswordReset(email); suspend fun signOut(); val currentUser: StateFlow<User?> }`
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/auth/User.kt` — domain model, NOT a vendor type
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/auth/AuthError.kt` — sealed class: `InvalidCredentials`, `UserNotFound`, `EmailAlreadyInUse`, `NetworkError`, `Unknown(cause: String)`
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/auth/BackendFlavor.kt` — `enum class BackendFlavor { FIREBASE, SUPABASE }` + `object AppBackend { val current: BackendFlavor get() = BuildKonfig.BACKEND }`
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/telemetry/AnalyticsGateway.kt`
  - `interface AnalyticsGateway { fun logEvent(name: String, params: Map<String, Any> = emptyMap()); fun setUserId(id: String?); fun setUserProperty(key: String, value: String?) }`
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/telemetry/CrashReporter.kt`
  - `interface CrashReporter { fun recordException(throwable: Throwable); fun log(message: String); fun setKey(key: String, value: String); fun setUserId(id: String?) }`

**Acceptance:**
- All 6 files compile
- Grep `shared/src/commonMain/**/*.kt` for `import dev.gitlive`, `import com.google.firebase`, or `import io.github.jan.supabase` — zero matches
- Interfaces have KDoc explaining they are provider-agnostic and must not leak SDK types
- **Cross-check written in the interface KDoc:** each method mentions how BOTH Firebase and Supabase satisfy it (e.g., `signIn(...)` — "Firebase: `signInWithEmailAndPassword`. Supabase: `auth.signInWith(Email) { email; password }`.")

**Watchouts:**
- This is the load-bearing abstraction. If any method has a Firebase-shaped or Supabase-shaped bias, the other adapter will need to fake something. Design against the intersection of both APIs, not the union.

---

### P1-04b: Add Firebase KMP dependencies + config files
**Section:** § 1.3, § 1.6 · **Effort:** M · **Depends on:** P1-04a

Add GitLive Firebase KMP wrappers: `dev.gitlive:firebase-auth`, `firebase-analytics`, `firebase-crashlytics`. Add `google-services.json` (Android) and `GoogleService-Info.plist` (iOS) from a Firebase project you create.

**Prerequisite:** you (the user) must create a Firebase project in [console.firebase.google.com](https://console.firebase.google.com), register an Android app (package `org.moashraf.sayva`) and iOS app (bundle ID matching your Xcode project), and download the two config files. This ticket cannot start until those files exist.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `shared/build.gradle.kts`, `androidApp/build.gradle.kts`
- New: `androidApp/google-services.json` (**gitignored initially** — commit to a private branch or use secrets management once repo becomes public)
- New: `iosApp/iosApp/GoogleService-Info.plist` (**gitignored initially**)
- Modify: `.gitignore`

**Acceptance:**
- Build succeeds on both platforms
- Firebase initialization succeeds on cold start (log line visible)
- No behavior change to any screen — adapters aren't written yet

**Watchouts:** GitLive is the KMP-friendly wrapper but has occasional lag behind upstream Firebase SDK versions. Pin the version.

---

### P1-04c: Add Supabase KMP dependencies + config (BuildKonfig)
**Section:** § 1.3 · **Effort:** M · **Depends on:** P1-04a

Add [supabase-kt](https://github.com/supabase-community/supabase-kt): `io.github.jan-tennert.supabase:auth-kt`, `io.github.jan-tennert.supabase:postgrest-kt` (for later data work). Set up BuildKonfig to inject `SUPABASE_URL` and `SUPABASE_ANON_KEY` from gradle.properties at build time, plus the `BACKEND` flag that `BackendFlavor` reads.

**Prerequisite:** you create a Supabase project at [supabase.com](https://supabase.com) (free tier). From project settings, copy Project URL and anon/public API key. Enable Email/Password provider in Authentication > Providers.

**Files:**
- Modify: `gradle/libs.versions.toml` — add supabase-kt versions + BuildKonfig plugin
- Modify: `shared/build.gradle.kts` — supabase deps + BuildKonfig config block reading gradle properties
- New: `gradle.properties` entries (via user, gitignored template): `sayva.backend=firebase`, `sayva.supabase.url=https://xxx.supabase.co`, `sayva.supabase.anonKey=eyJ...`
- New: `gradle.properties.template` (committed) — documents the required keys with placeholder values
- Modify: `.gitignore` — add `gradle.properties` if it contains secrets (or use a separate `secrets.properties`)

**Acceptance:**
- Build succeeds on both platforms
- `BuildKonfig.BACKEND` returns `"firebase"` by default, `"supabase"` when overridden in gradle.properties
- `BuildKonfig.SUPABASE_URL` and `BuildKonfig.SUPABASE_ANON_KEY` are readable at runtime
- No behavior change to any screen — adapters aren't written yet

**Watchouts:**
- supabase-kt versions move fast; pin to a specific release, not `+`
- Anon key is safe to embed in the client (that's what it's for), but treat URL + key as sensitive enough to gitignore during dev to avoid casual leaks
- BuildKonfig plugin (`com.codingfeline.buildkonfig`) is the KMP-friendly way to inject compile-time constants; use it instead of hand-rolled expect/actual constants

---

### P1-05: Declare permissions
**Section:** § 1.4 · **Effort:** S · **Depends on:** —

Add camera, microphone, and notification permissions to both platforms' manifests. No runtime request yet — that's P1-15/P1-27.

**Files:**
- Modify: `androidApp/src/main/AndroidManifest.xml`
  - `<uses-permission android:name="android.permission.CAMERA" />`
  - `<uses-permission android:name="android.permission.RECORD_AUDIO" />`
  - `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` (Android 13+)
- Modify: `iosApp/iosApp/Info.plist`
  - `NSCameraUsageDescription`
  - `NSMicrophoneUsageDescription`

**Acceptance:**
- Android build succeeds; permissions visible in APK manifest via `aapt dump permissions`
- iOS build succeeds; Info.plist entries verified in Xcode

---

### P1-06: Add `contentDescription` to `SymbolIcon`
**Section:** § 1.5 · **Effort:** S · **Depends on:** —

Add a required `contentDescription: String?` parameter to `SymbolIcon`. Passing `null` marks the icon as decorative. This forces every call site (~200+) to make an explicit choice — but this ticket doesn't fix the call sites yet (that's P1-30 through P1-36).

**Files:**
- Modify: `shared/src/commonMain/kotlin/org/moashraf/sayva/designsystem/SymbolIcon.kt`
- All call sites will fail to compile until later tickets — **temporarily** add a default `contentDescription: String? = null` to unblock the intermediate PRs, and remove the default in a cleanup ticket after all screens are updated

**Acceptance:**
- `SymbolIcon` API accepts `contentDescription`
- Semantics applied via `Modifier.semantics { contentDescription = it ?: "" }` when non-null, or `clearAndSetSemantics {}` when null
- Build still succeeds thanks to the temporary default

---

### P1-07: Scaffold `ml/` Python subproject
**Section:** § 1.7 · **Effort:** M · **Depends on:** —

Create the directory structure documented in the parent plan's "Repo Structure Decision" section. Pick between `uv` and `poetry` — recommend `uv` for speed and reproducibility.

**Files:**
- New: `ml/pyproject.toml` with `sayva_ml` package config, dependencies pinned:
  - `torch`, `mediapipe`, `numpy`, `pyyaml`, `tqdm`
  - Dev: `pytest`, `ruff`, `mypy`
- New: `ml/README.md` — how to set up, run training, export
- New: `ml/.python-version`
- New: `ml/src/sayva_ml/{__init__.py, data/, preprocessing/, models/, training/, evaluation/, export/}`
- New: `ml/configs/vocabulary.yaml` — start with 5 signs: `Hello`, `Thank you`, `Please`, `Sorry`, `Yes`
- New: `ml/tests/test_placeholder.py`
- Modify: root `.gitignore` — add `ml/.venv/`, `ml/models/checkpoints/`, `ml/models/exported/`, `ml/datasets/`, `ml/**/__pycache__/`, `ml/**/.ipynb_checkpoints/`

**Acceptance:**
- `cd ml && uv sync && uv run pytest` succeeds (with the placeholder test)
- `cd ml && uv run ruff check .` passes
- Directory layout matches the parent plan

---

### P1-08: CI workflow split
**Section:** § 1.7 · **Effort:** S · **Depends on:** P1-07

Two GitHub Actions workflow files with path filters — Kotlin PRs don't run Python training, Python PRs don't rebuild the KMP app.

**Files:**
- New: `.github/workflows/app-ci.yml`
  - Triggers on changes to: `androidApp/**`, `iosApp/**`, `shared/**`, `gradle/**`, `*.gradle.kts`, `settings.gradle.kts`
  - Steps: `./gradlew :shared:assemble :shared:testAndroidHostTest :androidApp:assembleDebug`
- New: `.github/workflows/ml-ci.yml`
  - Triggers on changes to: `ml/**`
  - Steps: `uv sync`, `uv run ruff check .`, `uv run mypy src/`, `uv run pytest`

**Acceptance:**
- A PR touching only `ml/` does not trigger `app-ci`
- A PR touching only `shared/` does not trigger `ml-ci`
- Both workflows pass on `main`

---

## Wave 2: Core Infrastructure

### P1-09: Install Koin in `App.kt`
**Section:** § 1.1 · **Effort:** S · **Depends on:** P1-01

Set up Koin `startKoin { modules(sayvaModule) }` in `App.kt`. Register one hello-world service (a `PlatformInfo` provider that just returns `getPlatform().name`) to prove wiring works. No repositories yet.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/di/Modules.kt`
- Modify: `shared/src/commonMain/kotlin/org/moashraf/sayva/App.kt` — wrap in `KoinApplication { ... }` composable
- Modify: `androidApp/.../MainActivity.kt` and `iosApp/.../ContentView.swift` if Koin needs platform init

**Acceptance:**
- App launches without crashing on both platforms
- Injected `PlatformInfo` can be resolved from any composable via `koinInject()`

---

### P1-10: SQLDelight schemas
**Section:** § 1.2 · **Effort:** M · **Depends on:** P1-02

Define `.sq` schema files for all persisted models. One `.sq` file per entity for clarity.

**Files:**
- New: `shared/src/commonMain/sqldelight/org/moashraf/sayva/db/HistoryItem.sq`
- New: `shared/src/commonMain/sqldelight/org/moashraf/sayva/db/FavoritePhrase.sq`
- New: `shared/src/commonMain/sqldelight/org/moashraf/sayva/db/SavedConversation.sq`
- New: `shared/src/commonMain/sqldelight/org/moashraf/sayva/db/Lesson.sq`
- New: `shared/src/commonMain/sqldelight/org/moashraf/sayva/db/ProgressStats.sq`
- New: `shared/src/commonMain/sqldelight/org/moashraf/sayva/db/UserProfile.sq`

Each includes: `CREATE TABLE`, basic `SELECT`/`INSERT`/`UPDATE`/`DELETE` queries.

**Acceptance:**
- Generated Kotlin classes appear in build output
- No runtime code uses them yet

---

### P1-11: Database driver expect/actual
**Section:** § 1.2 · **Effort:** M · **Depends on:** P1-02

Provide `SqlDriver` factory via expect/actual.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/db/DatabaseDriverFactory.kt` (expect)
- New: `shared/src/androidMain/kotlin/org/moashraf/sayva/data/db/DatabaseDriverFactory.android.kt`
- New: `shared/src/iosMain/kotlin/org/moashraf/sayva/data/db/DatabaseDriverFactory.ios.kt`
- Modify: `shared/src/commonMain/kotlin/org/moashraf/sayva/di/Modules.kt` — register `SayvaDatabase` in Koin

**Acceptance:**
- Database instance can be resolved from Koin on both platforms
- A trivial `SELECT 1` succeeds

---

### P1-12: Settings storage expect/actual
**Section:** § 1.2 · **Effort:** S · **Depends on:** P1-03

Wrap `multiplatform-settings` with a KMP-shared `SettingsStorage` interface. Register in Koin.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/prefs/SettingsStorage.kt`
- New: `androidMain` + `iosMain` actuals
- Modify: `di/Modules.kt`

**Acceptance:**
- `SettingsStorage.putBool("test", true)` and `getBool("test")` round-trip on both platforms

---

### P1-13: Secure storage expect/actual
**Section:** § 1.3 · **Effort:** M · **Depends on:** —

`SecureStorage` interface for auth tokens. Android → EncryptedSharedPreferences. iOS → Keychain.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/secure/SecureStorage.kt`
- New: `androidMain` + `iosMain` actuals
- Add `androidx.security:security-crypto` to Android dependencies

**Acceptance:**
- Round-trip test on both platforms passes
- Values persist across app restart

**Watchouts:** EncryptedSharedPreferences has silently broken on some Samsung devices in the past. Add a fallback: if init fails, log to Crashlytics and use plain SharedPreferences (with a warning). Documented decision.

---

### P1-14: Biometric prompt expect/actual
**Section:** § 1.3 · **Effort:** M · **Depends on:** —

Trigger biometric auth (fingerprint / Face ID). Returns success/fail/unavailable.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/auth/BiometricPrompt.kt`
- New: `androidMain` actual using `androidx.biometric:biometric`
- New: `iosMain` actual using `LocalAuthentication.LAContext`

**Acceptance:**
- Test screen (throwaway) can invoke biometric prompt and receive result on both platforms
- Graceful "unavailable" path when device has no biometric hardware

---

### P1-15: Permission controller expect/actual
**Section:** § 1.4 · **Effort:** M · **Depends on:** P1-05

Wraps camera / microphone / notification permission requests.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/permission/PermissionController.kt`
- New: `androidMain` actual using Activity result API
- New: `iosMain` actual using `AVCaptureDevice.requestAccess(for: .video)`, etc.

**Acceptance:**
- `PermissionController.request(Permission.Camera)` returns granted/denied/permanentlyDenied
- Works on both platforms without needing screen-side plumbing yet

---

### P1-16: Firebase adapters implementing the gateway interfaces
**Section:** § 1.3, § 1.6 · **Effort:** M · **Depends on:** P1-04a (interfaces), P1-04b (Firebase SDK + config)

Implement `AuthGateway`, `AnalyticsGateway`, `CrashReporter` interfaces using the GitLive Firebase KMP SDKs. **All Firebase-specific code lives in this ticket's files** — nothing else in the codebase should import `dev.gitlive.firebase.*`.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/firebase/FirebaseAuthGateway.kt`
  - Translates `FirebaseAuthException` → `AuthError` sealed class members
  - Maps `FirebaseUser` → domain `User` model
  - Exposes suspending API, not Firebase's callback style
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/firebase/FirebaseAnalyticsGateway.kt`
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/firebase/FirebaseCrashReporter.kt`
- Modify: `di/Modules.kt` — Analytics + Crash bindings are direct (`single<AnalyticsGateway> { FirebaseAnalyticsGateway() }`). Auth binding is a selector — see P1-16b.

**Acceptance:**
- Test screen (throwaway) resolves `AuthGateway` from Koin (with `BACKEND=firebase`), calls `signIn(...)`, receives normalized `User` or `AuthError`
- `AnalyticsGateway.logEvent("test_event")` shows up in Firebase console within 24 hours
- `CrashReporter.recordException(...)` visible in Crashlytics logs within 5 minutes
- Grep the codebase outside `data/firebase/` for `dev.gitlive` imports — should be zero results

**Watchouts:**
- Error mapping is where the abstraction succeeds or fails. If any `FirebaseAuthException` escapes into a ViewModel, the abstraction is broken. Add lint rule if possible.
- Token refresh logic stays inside `FirebaseAuthGateway`. The `AuthGateway` interface does not have a `refreshToken()` method — the adapter handles that transparently.

---

### P1-16b: Supabase Auth adapter + Koin selector
**Section:** § 1.3 · **Effort:** M · **Depends on:** P1-04a, P1-04c (Supabase SDK + config), P1-16 (so the selector has both options to pick from)

Implement `AuthGateway` using supabase-kt's Auth module. Add the Koin selector that binds either `FirebaseAuthGateway` or `SupabaseAuthGateway` based on `BackendFlavor.current`.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/supabase/SupabaseAuthGateway.kt`
  - Constructs a `SupabaseClient` from `BuildKonfig.SUPABASE_URL` + `SUPABASE_ANON_KEY`
  - Translates `RestException` / `AuthRestException` → `AuthError` sealed class members
  - Maps supabase `UserInfo` → domain `User` model
  - Manages its own token refresh internally via supabase-kt's session handling
- Modify: `di/Modules.kt` — replace the direct Firebase auth binding with:
  ```kotlin
  single<AuthGateway> {
      when (AppBackend.current) {
          BackendFlavor.FIREBASE -> FirebaseAuthGateway(get())
          BackendFlavor.SUPABASE -> SupabaseAuthGateway(get())
      }
  }
  ```

**Acceptance:**
- With `sayva.backend=firebase` in gradle.properties: sign-in flow hits Firebase, user appears in Firebase console
- With `sayva.backend=supabase`: sign-in flow hits Supabase, user appears in Supabase Authentication > Users
- Rebuilding with a flipped flag requires **zero code changes** — just the gradle.properties value
- Both adapters produce byte-identical `AuthError` values for equivalent failures (test: submit wrong password to both, both return `AuthError.InvalidCredentials`)
- Grep the codebase outside `data/supabase/` for `import io.github.jan.supabase` — should be zero results

**Watchouts:**
- Error taxonomies differ between Firebase and Supabase (e.g., Supabase returns HTTP status codes; Firebase returns typed exceptions). The `AuthError` mapping is the reconciliation point. If you can't map a Firebase error to the same `AuthError` variant that Supabase would produce, the interface needs adjustment — do it now, not later.
- Session persistence: Firebase manages its own persistent session automatically; supabase-kt requires you to install `SessionManager` explicitly. Wire it up inside `SupabaseAuthGateway`'s init.
- Test both paths in CI eventually — but for now, manual verification with both `gradle.properties` values is enough.

---

### P1-17: Vocabulary codegen pipeline
**Section:** § 1.7, § 2.3 · **Effort:** M · **Depends on:** P1-07

`ml/configs/vocabulary.yaml` is the single source of truth. A Gradle task reads it and generates `shared/src/commonMain/kotlin/org/moashraf/sayva/ml/generated/Vocabulary.kt`.

**Files:**
- Modify: `shared/build.gradle.kts` — new `generateVocabulary` task; wire before `compileKotlin`
- New: `buildSrc/src/main/kotlin/GenerateVocabulary.kt` (or `build-logic/`)
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/ml/generated/.gitignore` — ignore generated files

**Acceptance:**
- Editing `ml/configs/vocabulary.yaml` and running `./gradlew :shared:generateVocabulary` regenerates `Vocabulary.kt`
- Generated file is used by Kotlin (import it somewhere trivial to prove it compiles)
- CI fails if generated file is committed manually

---

### P1-18: Toy 5-sign training loop
**Section:** § 1.7 · **Effort:** L · **Depends on:** P1-07

Full train → eval cycle on 5 signs using a public dataset subset. LSTM on landmark sequences.

**Files (`ml/`):**
- New: `ml/src/sayva_ml/data/wlasl_loader.py`
- New: `ml/src/sayva_ml/preprocessing/landmarks.py` — MediaPipe landmark extraction
- New: `ml/src/sayva_ml/models/lstm.py`
- New: `ml/src/sayva_ml/training/train.py`
- New: `ml/src/sayva_ml/evaluation/metrics.py`
- New: `ml/tests/test_pipeline.py` — smoke test

**Acceptance:**
- `uv run python -m sayva_ml.training.train --config configs/toy.yaml` completes and writes a checkpoint to `ml/models/checkpoints/`
- Held-out accuracy > 50% on 5 signs (low bar — this is scaffolding)
- Confusion matrix logged

**Watchouts:** WLASL requires manual download and terms acceptance. Document this in `ml/README.md`.

---

### P1-19: TFLite + CoreML export prototype
**Section:** § 1.7 · **Effort:** L · **Depends on:** P1-18

Convert the trained toy checkpoint to TFLite and CoreML. Export goes to `ml/models/exported/<version>/`.

**Files:**
- New: `ml/src/sayva_ml/export/tflite_export.py`
- New: `ml/src/sayva_ml/export/coreml_export.py`
- New: `ml/src/sayva_ml/export/verify.py` — loads exported model, runs one inference, compares to PyTorch output

**Acceptance:**
- Exported `.tflite` and `.mlmodel` files verified numerically-equivalent to PyTorch checkpoint (within tolerance)
- Loading the TFLite model in Android instrumented test succeeds (throwaway test)
- Loading the CoreML model in iOS test succeeds (throwaway test)

**Watchouts:** TFLite ops may not be supported for some PyTorch layers. If conversion fails, switch to ONNX intermediate or restructure model.

---

## Wave 3: Feature Integration

### P1-20: Repository interfaces in `commonMain`
**Section:** § 1.1 · **Effort:** M · **Depends on:** P1-09

Define all 7 repository interfaces up front so ViewModels can code against them before implementations exist.

**Files:**
- New: `shared/src/commonMain/kotlin/org/moashraf/sayva/data/repository/{HistoryRepository, FavoritesRepository, ConversationsRepository, LessonsRepository, ProgressRepository, UserRepository, SettingsRepository}.kt`

Each is an interface only. Method signatures derived from what screens currently pull from `MockSayvaData`.

**Acceptance:**
- Interfaces compile
- No implementations yet

---

### P1-21: SettingsRepository + wire settings screens
**Section:** § 1.1, § 1.2 · **Effort:** M · **Depends on:** P1-12, P1-20

Implement `SettingsRepository` backed by `SettingsStorage`. Add `SettingsViewModel`. Wire `SettingsScreen` and `AccessibilityScreen` to it — all toggles now persist.

**Files:**
- New: `shared/src/.../data/repository/SettingsRepositoryImpl.kt`
- New: `shared/src/.../viewmodel/SettingsViewModel.kt`
- Modify: `SettingsScreen.kt`, `AccessibilityScreen.kt` — replace local `mutableStateOf` with ViewModel state
- Modify: `di/Modules.kt`

**Acceptance:**
- Toggle dark mode → kill app → relaunch → toggle persists
- Same for every setting on both screens

---

### P1-22: HistoryRepository + wire History screens
**Section:** § 1.1, § 1.2 · **Effort:** M · **Depends on:** P1-10, P1-11, P1-20

Implement `HistoryRepository` backed by SQLDelight. Add `HistoryViewModel`. On first launch, seed the database with `MockSayvaData.history` so the UI still shows the sample entries.

**Files:**
- New: `HistoryRepositoryImpl.kt`, `HistoryViewModel.kt`
- Modify: `HistoryScreen.kt`, `HistoryDetailScreen.kt`
- New: `shared/src/.../data/seed/SeedData.kt` — one-time seeding logic

**Acceptance:**
- App launches with 5 history entries visible
- Kill app → relaunch → entries still there
- Favorite toggle in HistoryDetail persists

---

### P1-23: FavoritesRepository + wire FavoritesScreen
**Section:** § 1.1, § 1.2 · **Effort:** M · **Depends on:** P1-10, P1-11, P1-20

Same pattern as P1-22 for favorites.

**Files:**
- New: `FavoritesRepositoryImpl.kt`, `FavoritesViewModel.kt`
- Modify: `FavoritesScreen.kt`
- Modify: `SeedData.kt`

**Acceptance:**
- 4 seeded favorites visible on first launch
- Emergency mode toggle persists
- Filter selection persists across screen navigation

---

### P1-24: ConversationsRepository + wire conversation screens
**Section:** § 1.1, § 1.2 · **Effort:** M · **Depends on:** P1-10, P1-11, P1-20

Wire `ConversationScreen` and `SavedConversationsScreen`. Saving from `ConversationScreen` persists to the repo.

**Files:**
- New: `ConversationsRepositoryImpl.kt`, `ConversationsViewModel.kt`
- Modify: `ConversationScreen.kt`, `SavedConversationsScreen.kt`
- Modify: `SeedData.kt`

**Acceptance:**
- 3 seeded conversations visible in `SavedConversationsScreen`
- "Stop & save" in `ConversationScreen` persists a mock conversation and it appears in the list

---

### P1-25: Learn repositories + wire 4 Learn screens
**Section:** § 1.1, § 1.2 · **Effort:** L · **Depends on:** P1-10, P1-11, P1-20

`LessonsRepository` + `ProgressRepository`. `LearnViewModel` + `PracticeViewModel` + `ProgressViewModel`. Wire `LearnCategoriesScreen`, `LessonPlayerScreen`, `PracticeScreen`, `ProgressScreen`.

**Files:**
- New: `LessonsRepositoryImpl.kt`, `ProgressRepositoryImpl.kt`
- New: `LearnViewModel.kt`, `PracticeViewModel.kt`, `ProgressViewModel.kt`
- Modify: all 4 screen files
- Modify: `SeedData.kt`

**Acceptance:**
- Categories render with real progress from repo
- Completing a practice question increments XP and persists
- Streak counter reflects real days-of-use (mocked clock for now)

---

### P1-26: Auth repositories + wire auth screens
**Section:** § 1.1, § 1.3 · **Effort:** L · **Depends on:** P1-04a, P1-13, P1-14, P1-16, P1-20

`AuthRepository` depends on `AuthGateway` (not on Firebase directly). `UserRepository` for profile data. Wire `LoginScreen`, `RegisterScreen`, `ForgotPasswordScreen`, `ResetEmailSentScreen`. **When Supabase migration happens later, no changes are needed in this ticket's files** — only the DI binding of `AuthGateway` changes.

**Files:**
- New: `AuthRepositoryImpl.kt` — constructor injects `AuthGateway`, `SecureStorage`, `BiometricPrompt`; no Firebase imports
- New: `UserRepositoryImpl.kt`
- New: `AuthViewModel.kt`
- Modify: all 4 auth screens
- Modify: `ProfileScreen.kt` — sign-out button hits `AuthRepository.signOut()`

**Acceptance:**
- Register with a test email → account created (visible in Firebase console today, would be visible in Supabase dashboard after swap)
- Sign in → session persists across app restart (secure storage tokens)
- Biometric sign-in works after initial password sign-in
- Guest mode → local-only user, no Firebase account
- Sign out from Profile returns to Welcome screen
- Grep `AuthRepositoryImpl.kt` and screen files for `import dev.gitlive` — should be zero results

---

### P1-27: Wire real permission requests
**Section:** § 1.4 · **Effort:** S · **Depends on:** P1-15

Hook `PermissionsScreen` up to `PermissionController`. Each row's "Allow" button triggers real system prompt.

**Files:**
- Modify: `PermissionsScreen.kt`

**Acceptance:**
- Camera permission shows real Android/iOS system dialog
- Granted state reflects real permission status (not hardcoded)
- Permanently denied state routes to system settings

---

### P1-28: Wire Analytics events + Crashlytics into ViewModels
**Section:** § 1.6 · **Effort:** M · **Depends on:** P1-16, P1-20

Log key events from ViewModels: `screen_view`, `sign_in_success`, `favorite_added`, `lesson_completed`, `paywall_viewed`, `paywall_purchased`.

**Files:**
- Modify: all ViewModels — call `Analytics.logEvent(...)` at appropriate points
- Modify: `SayvaNavController.kt` — log `screen_view` on every navigate

**Acceptance:**
- Firebase DebugView shows events flowing during a test session
- No PII in event payloads (audit before merge)

**Watchouts:** Do NOT log recognized signs or conversation content — that's private communication data. Documented in the parent plan's Security section.

---

### P1-29: Wire `CrashReportScreen` to real submission
**Section:** § 1.6 · **Effort:** S · **Depends on:** P1-16

The three data-category toggles now gate what's included in the submitted report.

**Files:**
- Modify: `CrashReportScreen.kt`

**Acceptance:**
- Tapping "Send report" submits to Crashlytics with the selected categories
- Report visible in Crashlytics console
- User-written note included as custom key

---

### P1-30: Semantics for shared components
**Section:** § 1.5 · **Effort:** S · **Depends on:** P1-06

Add semantics to `SayvaTopBar`, `SayvaBottomNav`, `PrimaryButton`, `SecondaryButton`, `TextLink`, `Pill`.

**Files:**
- Modify: `ui/components/Bars.kt`, `Buttons.kt`, `Misc.kt`

**Acceptance:**
- TalkBack announces button roles and labels correctly
- Bottom nav tabs announce as tabs with selected state

---

### P1-31 through P1-36: Semantics per screen group
**Section:** § 1.5 · **Effort:** M–L each · **Depends on:** P1-06, P1-30

Six tickets, one per screen category. Each ticket:
- Passes explicit `contentDescription` (or `null` for decorative) to every `SymbolIcon`
- Adds `Modifier.semantics { ... }` where needed for grouping, live regions, roles
- Uses `Modifier.clearAndSetSemantics {}` for decorative gradient overlays and detection brackets

| Ticket | Screens |
|---|---|
| P1-31 | Welcome, HowAiWorks, TwoWayIntro, Permissions, Login, Register, ForgotPassword, ResetEmailSent |
| P1-32 | Home, LiveCamera, Conversation, AiFeedbackLowConfidence |
| P1-33 | History, HistoryDetail, Favorites, SavedConversations |
| P1-34 | LearnCategories, LessonPlayer, Practice, Progress |
| P1-35 | Profile, Settings, Accessibility, Notifications |
| P1-36 | Contribute, OfflineModels, SystemStates, FirstLaunchModelDownload, PairSecondScreen, Paywall, Family, CrashReport, InterpreterHandoff (9 screens, hence `L`) |

**Acceptance per ticket:** TalkBack walkthrough of every screen in the group announces every element. No "double talk" (unlabeled buttons or duplicate reads).

---

### Cleanup (bundled into P1-36 or a follow-up)
- Remove the temporary default from `SymbolIcon.contentDescription` after all call sites are explicit
- Add a lint rule (or a CI check) that fails if a new `SymbolIcon(...)` call omits `contentDescription`

---

## Wave 4: Verification

### P1-37: Phase 1 verification walkthrough
**Section:** all of § 1 · **Effort:** M · **Depends on:** all Wave 3 tickets

Full checklist walkthrough per the parent plan's "Phase 1 Verification" section. This is a **release checklist**, not a coding ticket.

**Checklist:**
1. Rotate device on every screen — state must persist (config-change survival)
2. Kill and cold-start the app — favorites, history, saved conversations, settings all still there
3. Sign in → sign out → sign in again → session and biometric both work
4. TalkBack walkthrough of the full navigation graph — every element announced correctly
5. Force a crash via a debug menu button — Crashlytics dashboard shows the stack trace within 5 minutes
6. `ml/` project runs full train → evaluate → export cycle on 5-sign toy vocab
7. `ml/configs/vocabulary.yaml` edit → `Vocabulary.kt` regenerates correctly

**Non-code checklist items** (external — do not block the ticket but must be checked):
- ML partner contract signed OR clear go-solo-with-in-house decision documented
- At least one content licensing conversation reached term-sheet stage
- Developer accounts fully provisioned, sandbox products stubbed

**Acceptance:**
- All 7 code items pass
- All 3 external items either done or explicitly deferred with owner and target date
- Verification results documented in `docs/phase-1-verification-<date>.md`

---

## External / Non-Code Work

Parallel to all waves — these are called out in the plan but produce no code:

- **Register App Store Connect account** — owner: TBD · target: end of Wave 1
- **Register Google Play Console account** — owner: TBD · target: end of Wave 1
- **Provision Firebase project** (production + staging) — owner: TBD · target: middle of Wave 1
- **Initiate content licensing conversations** — owner: TBD · target: kickoff Wave 1
- **ML partner RFP / go-solo decision** — owner: TBD · target: mid-Wave 2

None of these block coding tickets, but Wave 4 verification depends on them being underway.

---

## Risks & Watchouts

Called out in the parent plan; these are the ones most likely to bite during Phase 1 specifically:

1. **GitLive Firebase KMP lag** (P1-04) — pin versions carefully; if the wrapper doesn't support a needed Firebase feature, fall back to expect/actual with platform SDKs directly
2. **EncryptedSharedPreferences unreliability on old Samsung devices** (P1-13) — plan the fallback path from day one
3. **WLASL dataset licensing / download friction** (P1-18) — document manual steps clearly in `ml/README.md`
4. **TFLite op compatibility** (P1-19) — if PyTorch → TFLite direct conversion fails, ONNX intermediate is the escape hatch
5. **`SymbolIcon` contentDescription default footgun** (P1-06) — the temporary default is a hack. Track it; remove it in the cleanup step; add a lint rule
6. **Firebase config file leakage** (P1-04) — `google-services.json` and `GoogleService-Info.plist` must be gitignored; use secrets management for CI

---

## What This Document Doesn't Cover

- Detailed test plans per repository (deferred to individual ticket authors)
- Design QA for any screens that visually change (add to each screen ticket as needed)
- Rollout / release plan (Phase 1 is not user-facing — no rollout needed)
- Cross-cutting concerns from the parent plan (localization, dark theme, feature modules) — those are cross-cutting and not Phase 1 scope even though they're mentioned in the parent

**Next document to produce (after Phase 1 completion, not before):** `docs/phase-2-tickets.md`, generated the same way from Phase 2's sections.
