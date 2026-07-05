# Changelog

All notable changes to Sayva are documented in this file.

---

## [1.0.0] — Unreleased (UI Prototype)

### Summary

Initial UI prototype with all 33 screens built using Compose Multiplatform. All screens are visually complete with production-quality design, but no backend, AI, or data persistence is implemented.

### Added

**Onboarding (8 screens)**
- Welcome screen with brand hero and sign language icon
- How AI Works explainer with 3-step pipeline visualization
- Two-Way Intro with mock conversation demo
- Permissions screen (camera, mic, notifications)
- Login with email/password, biometric, and guest mode
- Registration with password strength indicator and language selector
- Forgot Password flow with email input
- Reset Email Sent confirmation with resend countdown

**Home & Translation (4 screens)**
- Home dashboard with greeting, search, quick actions grid, daily challenge
- Live Camera screen with detection brackets, FPS counter, translation result card
- Conversation mode with sign/voice transcript bubbles and recording controls
- AI Feedback screen for low-confidence results with suggestions

**Memory (4 screens)**
- Translation History with search, filter chips, grouped list
- History Detail with video placeholder, metadata, TTS playback, action tiles
- Favorites phrase board with gradient cards, emergency mode toggle
- Saved Conversations with search and highlighted cards

**Learning (4 screens)**
- Learn Categories with 6 categories, progress tracking, daily challenge
- Lesson Player with video placeholder, playback controls, sign tags
- Practice quiz with prompt cards, feedback chips, match scoring
- Progress dashboard with streak, stats, weekly chart, badges

**Profile & Settings (4 screens)**
- User Profile with avatar, stats, 9-item navigation menu
- Settings with display, speech, camera/AI, diagnostics sections
- Accessibility with 8 configurable options (UI only)
- Notifications with grouped list and visual-first design

**System (3 screens)**
- Contribute to AI with impact stats, daily missions, upload progress
- Offline Models with storage chart, pack management, download toggles
- System States reference showing empty/loading/error/success patterns

**Critical (6 screens)**
- First Launch Model Download with step tracker and progress visualization
- Paywall with 3 subscription tiers and feature comparison
- Family sharing with member list, invites, parental controls
- Crash Report with toggleable data categories and privacy assurance
- Interpreter Handoff with VRS service options and safety warning
- Second Screen Pairing with mock QR code and device connection

**Design System**
- Custom color palette: Electric Indigo primary, Warm Coral secondary, Signal Green tertiary
- Plus Jakarta Sans typography (6 weights, 13 text styles)
- Material Symbols Rounded icon font (85+ icons, filled + outline variants)
- SymbolIcon composable for glyph rendering
- Custom spacing scale (4dp–48dp) and shape tokens
- SayvaTheme wrapping Material3

**Shared Components**
- SayvaTopBar (back + title + trailing slot)
- SayvaBottomNav (4-tab with active/inactive states)
- PrimaryButton (solid pill, full-width)
- SecondaryButton (outlined pill)
- TextLink (inline clickable text)
- Pill (small rounded label chip)

**Navigation**
- Custom SayvaNavController with push/pop/replaceAll
- 33-screen sealed class hierarchy in Screen.kt
- Bottom navigation visible only on 4 root screens

**Platform Integration**
- Text-to-Speech via expect/actual (Android TextToSpeech, iOS AVSpeechSynthesizer)
- Platform name/version via expect/actual
- iOS MainViewController bridge to SwiftUI

**Data Layer**
- 13 data model classes covering all features
- MockSayvaData object with complete sample datasets

### Not Yet Implemented

- Camera integration (CameraX / AVCaptureSession)
- ML model inference (sign recognition)
- Backend API / authentication
- Local database / data persistence
- Settings persistence
- Payment processing (StoreKit / Google Play Billing)
- Push notifications
- Crash reporting SDK
- Analytics
- Accessibility semantics (Compose Modifier.semantics)
- Dark theme
- Localization / i18n
