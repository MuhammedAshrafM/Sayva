# Architecture

## Table of Contents

- [Module Structure](#module-structure)
- [Shared Module Source Sets](#shared-module-source-sets)
- [Package Layout](#package-layout)
- [Navigation System](#navigation-system)
- [Screen Hierarchy](#screen-hierarchy)
- [Platform Abstraction](#platform-abstraction)
- [Theme and Composition](#theme-and-composition)
- [Dependency Graph](#dependency-graph)

---

## Module Structure

```
Sayva/
├── shared/              # All business logic, UI, navigation, design system
├── androidApp/          # Thin Android wrapper (MainActivity → App())
└── iosApp/              # Xcode project (ContentView.swift → MainViewController())
```

### `:shared`

Contains 100% of the application logic and UI. Both platform apps are thin entry points that host the shared Compose content.

### `:androidApp`

Single `MainActivity` that calls `setContent { App() }`. No Android-specific UI.

### `iosApp`

Swift/Xcode project. `ContentView.swift` wraps Kotlin's `MainViewController()` factory function defined in `iosMain`.

---

## Shared Module Source Sets

| Source Set | Path | Purpose |
|---|---|---|
| `commonMain` | `shared/src/commonMain/` | All shared UI, navigation, data, design system |
| `androidMain` | `shared/src/androidMain/` | `actual` implementations for TTS, Platform |
| `iosMain` | `shared/src/iosMain/` | `actual` implementations for TTS, Platform, MainViewController |
| `commonTest` | `shared/src/commonTest/` | Shared tests |
| `androidHostTest` | `shared/src/androidHostTest/` | Android-specific tests |
| `iosTest` | `shared/src/iosTest/` | iOS-specific tests |

---

## Package Layout

```
org.moashraf.sayva/
├── App.kt                          # Root composable, screen router
├── Platform.kt                     # expect Platform interface
├── speech/
│   └── Speech.kt                   # expect fun speakText(text: String)
├── nav/
│   ├── Screen.kt                   # Sealed class hierarchy (33 screens)
│   └── SayvaNavController.kt       # Custom back-stack navigation
├── data/
│   ├── Models.kt                   # 13 data classes
│   └── MockSayvaData.kt            # All mock data
├── designsystem/
│   ├── Color.kt                    # Full color palette
│   ├── Theme.kt                    # SayvaTheme composable
│   ├── Type.kt                     # Typography + font families
│   ├── Shape.kt                    # Spacing and shape tokens
│   ├── Icons.kt                    # MaterialSymbol codepoint map (85+ icons)
│   └── SymbolIcon.kt               # Icon rendering composable
└── ui/
    ├── components/
    │   ├── Bars.kt                 # SayvaTopBar, SayvaBottomNav
    │   ├── Buttons.kt              # PrimaryButton, SecondaryButton, TextLink
    │   └── Misc.kt                 # Pill chip component
    └── screens/
        ├── onboarding/             # Screens 01–08
        ├── home/                   # Screens 09–12
        ├── memory/                 # Screens 13–16
        ├── learn/                  # Screens 17–20
        ├── you/                    # Screens 21–24
        ├── system/                 # Screens 25–27
        └── critical/               # Screens 28–33
```

---

## Navigation System

### SayvaNavController

A custom lightweight navigation controller using `mutableStateListOf` as a back-stack. This was chosen intentionally over `androidx.navigation` to minimize binary size.

**Operations:**

| Method | Behavior |
|---|---|
| `navigate(screen)` | Push screen onto back-stack |
| `back()` | Pop top screen; no-op if stack has one item |
| `replaceAll(screen)` | Clear stack and set single screen |

**Initial screen:** `Screen.Welcome`

### Bottom Navigation

Four tabs defined in `BottomTab` enum, each mapping to a root screen:

| Tab | Icon | Screen |
|---|---|---|
| Home | `home` | `Screen.Home` |
| Translate | `videocam` | `Screen.LiveCamera` |
| Learn | `school` | `Screen.LearnCategories` |
| You | `person` | `Screen.Profile` |

The bottom navigation bar is only visible when the current screen is one of the four root screens. Tab selection calls `replaceAll()` to reset the stack.

### Screen Routing

`App.kt` contains a `RenderScreen()` function with a `when` expression that maps every `Screen` subclass to its composable. All 33 screens receive the `SayvaNavController` as their only parameter (some also receive data from the screen's properties, e.g., `HistoryDetail.entryId`).

---

## Screen Hierarchy

```
Screen (sealed class)
├── Onboarding
│   ├── Welcome
│   ├── HowAiWorks
│   ├── TwoWayIntro
│   ├── Permissions
│   ├── Login
│   ├── Register
│   ├── ForgotPassword
│   └── ResetEmailSent
├── Home & Translation
│   ├── Home
│   ├── LiveCamera
│   ├── Conversation
│   └── AiFeedbackLowConfidence
├── Memory
│   ├── History
│   ├── HistoryDetail(entryId: String)
│   ├── Favorites
│   └── SavedConversations
├── Learn
│   ├── LearnCategories
│   ├── LessonPlayer(lessonId: String)
│   ├── Practice(lessonId: String)
│   └── Progress
├── You
│   ├── Profile
│   ├── Settings
│   ├── Accessibility
│   └── Notifications
├── System
│   ├── Contribute
│   ├── OfflineModels
│   └── SystemStates
└── Critical
    ├── FirstLaunchModelDownload
    ├── PairSecondScreen
    ├── Paywall
    ├── Family
    ├── CrashReport
    └── InterpreterHandoff
```

---

## Platform Abstraction

Two `expect`/`actual` contracts bridge platform-specific functionality:

### Platform

```kotlin
// commonMain
expect class Platform() {
    val name: String
}
```

Returns a human-readable platform name (e.g., "Android 14", "iOS 17.0").

### Speech (Text-to-Speech)

```kotlin
// commonMain
expect fun speakText(text: String)
```

| Platform | Implementation |
|---|---|
| Android | `android.speech.tts.TextToSpeech` |
| iOS | `AVFoundation.AVSpeechSynthesizer` |

Used by `LiveCameraScreen` ("Speak aloud" button) and `HistoryDetailScreen` ("Spoken as" playback).

---

## Theme and Composition

The composition tree follows this structure:

```
SayvaTheme (MaterialTheme with custom colorScheme, typography, shapes)
└── Column
    ├── RenderScreen(currentScreen, navController)
    └── SayvaBottomNav (conditional — only on root screens)
```

`SayvaTheme` applies:
- **Color scheme** — `SayvaColorScheme` (custom `lightColorScheme`)
- **Typography** — `sayvaTypography()` (Plus Jakarta Sans, 13 text styles)
- **Shapes** — `SayvaShapes` (custom rounded corner values)

---

## Dependency Graph

```
androidApp ──→ shared (commonMain + androidMain)
iosApp     ──→ shared (commonMain + iosMain)
```

**External dependencies** (from `libs.versions.toml`):
- Kotlin 2.4.0
- Compose Multiplatform 1.11.1
- Material3 (via Compose)
- Compose Resources (fonts, icons)
- AGP 9.0.1

**No runtime dependencies on:** networking libraries, databases, DI frameworks, image loaders, analytics SDKs, or navigation libraries.
