# API & Platform Interfaces

## Table of Contents

- [Overview](#overview)
- [Platform Interface](#platform-interface)
- [Text-to-Speech API](#text-to-speech-api)
- [MainViewController (iOS)](#mainviewcontroller-ios)
- [Future API Surface](#future-api-surface)

---

## Overview

Sayva uses Kotlin Multiplatform's `expect`/`actual` mechanism to abstract platform-specific APIs. Currently, two contracts exist:

| Contract | Defined In | Purpose | Status |
|---|---|---|---|
| `Platform` | `Platform.kt` | Platform name/version string | Implemented |
| `speakText()` | `speech/Speech.kt` | Text-to-speech playback | Implemented |
| `MainViewController()` | `iosMain` only | iOS Compose entry point | Implemented |

No network APIs, REST clients, or external service integrations exist.

---

## Platform Interface

### Contract (commonMain)

```kotlin
// shared/src/commonMain/kotlin/org/moashraf/sayva/Platform.kt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
```

### Android Implementation

```kotlin
// shared/src/androidMain/.../Platform.android.kt

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
```

Returns SDK int version (e.g., "Android 34").

### iOS Implementation

```kotlin
// shared/src/iosMain/.../Platform.ios.kt

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()
```

Returns system name and version (e.g., "iOS 17.0").

### Usage

Not actively used in any screen. Available for diagnostic/about screens.

---

## Text-to-Speech API

### Contract (commonMain)

```kotlin
// shared/src/commonMain/kotlin/org/moashraf/sayva/speech/Speech.kt

expect fun speakText(text: String)
```

A top-level function that speaks the given text string aloud using the platform's TTS engine.

### Android Implementation

```kotlin
// shared/src/androidMain/.../speech/Speech.android.kt

object AndroidSpeech {
    var engine: TextToSpeech? = null

    fun init(context: Context) {
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
            }
        }
    }
}

actual fun speakText(text: String) {
    AndroidSpeech.engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sayva-utterance")
}
```

**Initialization:** Must call `AndroidSpeech.init(context)` from `MainActivity.onCreate()`.

**Behavior:**
- Queue mode: `QUEUE_FLUSH` — interrupts any in-progress speech
- Locale: `Locale.US` (American English)
- Utterance ID: `"sayva-utterance"`

### iOS Implementation

```kotlin
// shared/src/iosMain/.../speech/Speech.ios.kt

private val synthesizer = AVSpeechSynthesizer()

actual fun speakText(text: String) {
    synthesizer.speakUtterance(AVSpeechUtterance(string = text))
}
```

**Behavior:**
- Creates a new `AVSpeechUtterance` for each call
- Uses system default voice and rate
- Lazy-initialized singleton synthesizer

### Usage Points

| Screen | Trigger | Text Source |
|---|---|---|
| LiveCameraScreen | "Speak aloud" button | Hardcoded "Thank you" |
| FavoritesScreen | Speak button on phrase card | `FavoritePhrase.text` |
| HistoryDetailScreen | Speaker icon button | `HistoryDetail.spokenAs` |
| ConversationScreen | Tap on transcript bubble | Bubble text content |

---

## MainViewController (iOS)

### Factory Function

```kotlin
// shared/src/iosMain/.../MainViewController.kt

fun MainViewController() = ComposeUIViewController { App() }
```

Creates a `UIViewController` hosting the Compose Multiplatform `App()` composable. Called from Swift via `MainViewControllerKt.MainViewController()`.

### Swift Integration

```swift
// iosApp/iosApp/ContentView.swift

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView().ignoresSafeArea()
    }
}
```

---

## Future API Surface

The following APIs are implied by the UI but not yet implemented. These represent the integration points needed to bring the app from prototype to production.

### Camera API

| Endpoint | Purpose | Screens |
|---|---|---|
| Camera stream | Live video feed for sign detection | LiveCamera |
| Frame capture | Individual frames for ML inference | LiveCamera, Practice |
| Camera switch | Front/back camera toggle | LiveCamera |
| Flash control | Torch toggle | LiveCamera |

### ML Inference API

| Endpoint | Purpose | Screens |
|---|---|---|
| `recognizeSign(frame)` | Real-time sign recognition | LiveCamera |
| `getConfidence()` | Recognition confidence score | LiveCamera, AiFeedback |
| `getSuggestions()` | Alternative sign suggestions | AiFeedback |
| `evaluatePractice()` | Practice attempt evaluation | Practice |

### Authentication API

| Endpoint | Purpose | Screens |
|---|---|---|
| `signIn(email, password)` | User authentication | Login |
| `register(name, email, password)` | Account creation | Register |
| `resetPassword(email)` | Password reset request | ForgotPassword |
| `signInBiometric()` | Biometric authentication | Login |
| `guestSignIn()` | Anonymous session | Login |

### Storage API

| Endpoint | Purpose | Screens |
|---|---|---|
| Save history | Persist translation history | LiveCamera → History |
| Save favorites | Persist favorite phrases | Favorites |
| Save conversations | Persist conversation transcripts | Conversation |
| Save progress | Persist learning progress | Practice, Progress |
| Save settings | Persist user preferences | Settings, Accessibility |

### Payment API

| Endpoint | Purpose | Screens |
|---|---|---|
| `getProducts()` | Fetch subscription plans | Paywall |
| `purchase(planId)` | Initiate purchase | Paywall |
| `restorePurchases()` | Restore prior purchases | Paywall |
| `checkEntitlement()` | Verify subscription status | Profile |

### Model Download API

| Endpoint | Purpose | Screens |
|---|---|---|
| `downloadModel(packId)` | Download language pack | OfflineModels, FirstLaunch |
| `getDownloadProgress()` | Progress reporting | FirstLaunch |
| `verifyModel(packId)` | Integrity check | FirstLaunch |
| `deleteModel(packId)` | Remove downloaded pack | OfflineModels |

### Interpreter Services API

| Endpoint | Purpose | Screens |
|---|---|---|
| `getInterpreters()` | List available services | InterpreterHandoff |
| `initiateCall(serviceId)` | Start VRS call | InterpreterHandoff |

### Crash Reporting API

| Endpoint | Purpose | Screens |
|---|---|---|
| `submitReport(data)` | Send crash report | CrashReport |

### Family API

| Endpoint | Purpose | Screens |
|---|---|---|
| `getFamily()` | List family members | Family |
| `inviteMember(method)` | Send invite | Family |
| `setParentalControls()` | Configure content filters | Family |

### Contribution API

| Endpoint | Purpose | Screens |
|---|---|---|
| `uploadClip(video)` | Submit training data | Contribute |
| `getMissions()` | Fetch daily recording tasks | Contribute |
| `getImpactStats()` | User contribution stats | Contribute |
