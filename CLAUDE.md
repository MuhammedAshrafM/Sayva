# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Sayva** is a Kotlin Multiplatform (KMP) sign language translation app targeting Android and iOS. It uses Compose Multiplatform for shared UI across both platforms.

## Build Commands

```bash
# Android
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug

# Tests
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test

# Language Packs (see docs/PACKS_WORKFLOW.md)
./gradlew :shared:generatePacks   # regenerate distributed manifest.json + models
./gradlew :shared:verifyPacks     # dry-run: fail if regeneration would rewrite anything

# iOS: open in Xcode and run from there
open iosApp/iosApp.xcodeproj
```

Gradle wrapper is at `./gradlew` (Unix) or `gradlew.bat` (Windows). Gradle 9.1.0, configuration cache and build cache are both enabled.

## Architecture

Three Gradle modules:
- **`:shared`** — all business logic, UI, navigation, and design system (Compose Multiplatform)
- **`:androidApp`** — thin wrapper; `MainActivity` calls `setContent { App() }`
- **`iosApp`** — Xcode project; `ContentView.swift` wraps Kotlin's `MainViewController()`

### Shared Module Source Sets

| Source Set | Purpose |
|---|---|
| `commonMain` | All shared UI, navigation, data models, design system |
| `androidMain` | `expect`/`actual` for TTS (`TextToSpeech`) and platform info |
| `iosMain` | `expect`/`actual` for TTS (`AVSpeechSynthesizer`) and `MainViewController` |
| `commonTest` / `androidHostTest` / `iosTest` | Tests per platform |

### Navigation

Uses a custom lightweight `SayvaNavController` (`nav/SayvaNavController.kt`) — a `mutableStateListOf` back-stack — instead of `androidx.navigation`. All 33 screens are declared as a sealed class hierarchy in `nav/Screen.kt`. Push/pop/replaceAll are the only navigation operations. Avoid adding `androidx.navigation` as a dependency; the custom controller is intentional to minimize binary size.

### Platform Abstraction

`expect`/`actual` pattern is used for two interfaces:
- `Platform` — returns platform name/version string
- `Speech` — text-to-speech; Android uses `android.speech.tts.TextToSpeech`, iOS uses `AVFoundation.AVSpeechSynthesizer`

Both are in `shared/src/commonMain/Platform.kt` and `Speech.kt`, with implementations in `androidMain` and `iosMain`.

### Data Layer

All data is mocked via `data/MockSayvaData.kt`. There is no database, no network client, and no dependency injection framework. Data models live in `data/Models.kt`.

### Design System

Located in `shared/src/commonMain/designsystem/`. Uses Material Design 3 with:
- Custom color palette in `Color.kt`
- Plus Jakarta Sans font (6 weights loaded as Compose resources)
- Material Symbols icons (filled + outline) loaded as font resources in `composeResources/`
- `SymbolIcon.kt` wraps the icon font for use in Compose

## Key Versions

Managed via `gradle/libs.versions.toml`:
- Kotlin: **2.4.0**
- AGP: **9.0.1**
- Compose Multiplatform: **1.11.1**
- Android compileSdk/targetSdk: **36**, minSdk: **24**
- JVM target: **11**
