# Sayva Documentation

> **Sayva** — A Kotlin Multiplatform sign language translation app for Android and iOS, powered by on-device AI and built with Compose Multiplatform.

---

## Table of Contents

| Document | Description |
|---|---|
| [Features](FEATURES.md) | Complete feature inventory with screens, states, and business rules |
| [User Flows](USER_FLOWS.md) | End-to-end user journeys through the app |
| [UI Components](UI_COMPONENTS.md) | Shared composable components and their usage |
| [Design System](DESIGN_SYSTEM.md) | Colors, typography, iconography, spacing, and shapes |
| [Architecture](ARCHITECTURE.md) | Module structure, navigation, platform abstraction |
| [API](API.md) | Platform APIs and expect/actual contracts |
| [AI Pipeline](AI_PIPELINE.md) | Sign language recognition pipeline design |
| [Data Model](DATA_MODEL.md) | Data classes, mock data, and storage |
| [Accessibility](ACCESSIBILITY.md) | Accessibility features and compliance |
| [Language Pack Workflow](PACKS_WORKFLOW.md) | Language-agnostic pack authoring, validation, and CI roadmap |
| [Changelog](CHANGELOG.md) | Version history |
| [Roadmap](ROADMAP.md) | Planned features and improvement recommendations |

---

## Quick Start

### Android

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
```

### iOS

```bash
open iosApp/iosApp.xcodeproj
# Build and run from Xcode
```

### Tests

```bash
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
```

---

## Project at a Glance

| Aspect | Detail |
|---|---|
| **Platforms** | Android (API 24+), iOS |
| **Language** | Kotlin 2.4.0 |
| **UI Framework** | Compose Multiplatform 1.11.1 |
| **Design System** | Material Design 3, Plus Jakarta Sans, Material Symbols |
| **Navigation** | Custom lightweight SayvaNavController (no androidx.navigation) |
| **Screens** | 33 screens across 7 groups |
| **Data Layer** | All mocked — no network, no database |
| **AI/ML** | Designed but not implemented — all values are hardcoded |
| **Build System** | Gradle 9.1.0 with configuration cache |

---

## Implementation Status

This project is in **UI prototype** stage. All screens are fully built with production-quality visuals, but:

- **No real AI/ML inference** — confidence scores, translations, and detection results are hardcoded
- **No backend or database** — all data comes from `MockSayvaData`
- **No camera integration** — the live camera screen is a visual mockup
- **No authentication** — login/register screens are visual only
- **No payments** — the paywall screen is a design reference
- **Text-to-speech is implemented** — the only functional platform feature via `expect`/`actual`

See [Roadmap](ROADMAP.md) for what needs to be built to reach production.
