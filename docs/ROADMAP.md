# Roadmap

## Table of Contents

- [Current State](#current-state)
- [Phase 1: Foundation](#phase-1-foundation)
- [Phase 2: Core AI](#phase-2-core-ai)
- [Phase 3: Learning Platform](#phase-3-learning-platform)
- [Phase 4: Social & Premium](#phase-4-social--premium)
- [Phase 5: Scale](#phase-5-scale)
- [Known Limitations](#known-limitations)
- [Missing Features (PM Assessment)](#missing-features-pm-assessment)
- [Improvement Recommendations](#improvement-recommendations)

---

## Current State

Sayva is a **UI prototype** with 33 fully designed screens and one functional platform feature (TTS). The visual design is production-quality, but no core functionality is implemented beyond screen rendering and navigation.

**What works today:**
- All 33 screens render correctly on Android and iOS
- Navigation between all screens via custom SayvaNavController
- Text-to-speech via platform-native engines
- Bottom navigation with 4 tabs
- Design system with custom colors, typography, and icons

**What does not work:**
- No camera input
- No sign language recognition
- No data persistence
- No user accounts
- No payments
- No analytics

---

## Phase 1: Foundation

**Goal:** Core infrastructure needed before any feature can function.

### 1.1 Data Persistence
- [ ] Add SQLDelight or Room (Android) / CoreData (iOS) via expect/actual
- [ ] Migrate MockSayvaData to real repositories
- [ ] Persist user settings (dark mode, accessibility, speech preferences)
- [ ] Persist favorites, history, saved conversations

### 1.2 Authentication
- [ ] Backend API for sign-in, registration, password reset
- [ ] Session management and token storage (Keychain/KeyStore)
- [ ] Biometric authentication (BiometricPrompt / LAContext)
- [ ] Guest mode with local-only data
- [ ] Social sign-in (Google, Apple)

### 1.3 Permissions
- [ ] Camera permission request flow (Android runtime permissions, iOS Info.plist)
- [ ] Microphone permission (optional, for voice input)
- [ ] Notification permission (Android 13+ POST_NOTIFICATIONS)
- [ ] Graceful degradation when permissions denied

### 1.4 Analytics & Crash Reporting
- [ ] Integrate crash reporting (Firebase Crashlytics or Sentry)
- [ ] Wire CrashReportScreen to real crash data submission
- [ ] Basic analytics events (screen views, feature usage)
- [ ] Privacy-compliant data collection (GDPR, CCPA)

### 1.5 Accessibility Fundamentals
- [ ] Add Compose `semantics` annotations to all interactive elements
- [ ] Add `contentDescription` to all SymbolIcon instances
- [ ] Test with TalkBack (Android) and VoiceOver (iOS)
- [ ] Wire accessibility toggles to real behavior

---

## Phase 2: Core AI

**Goal:** Working sign-to-text translation pipeline.

### 2.1 Camera Integration
- [ ] CameraX (Android) / AVCaptureSession (iOS) via expect/actual
- [ ] Live camera preview in LiveCameraScreen
- [ ] Front/back camera switching
- [ ] Flash/torch control
- [ ] Frame rate optimization targeting 30+ FPS

### 2.2 Hand Detection
- [ ] MediaPipe Hands integration or custom hand landmark model
- [ ] Real-time bounding box for detection brackets
- [ ] Hand presence detection (show/hide detection UI)
- [ ] Multi-hand support

### 2.3 Sign Recognition Model
- [ ] TFLite (Android) / CoreML (iOS) model for ASL classification
- [ ] Vocabulary: start with top 100 ASL signs
- [ ] Confidence scoring with real probabilities
- [ ] Temporal smoothing to reduce flickering
- [ ] Top-N suggestions for low confidence

### 2.4 Pipeline Integration
- [ ] Camera → detection → recognition → UI state flow
- [ ] Latency measurement and display
- [ ] Error handling (camera unavailable, model loading, low light)
- [ ] Background thread inference

### 2.5 Conversation Mode
- [ ] Bi-directional: sign input + speech recognition (voice → text)
- [ ] Transcript accumulation and persistence
- [ ] Conversation export (text file, share)

---

## Phase 3: Learning Platform

**Goal:** Interactive sign language education with real AI evaluation.

### 3.1 Lesson Content
- [ ] Expand from 1 lesson to full curriculum (100+ signs across 6 categories)
- [ ] Video demonstrations (real sign language videos)
- [ ] Slow-motion playback support
- [ ] Step-by-step breakdown of complex signs

### 3.2 Practice Evaluation
- [ ] Compare user gesture to reference model
- [ ] Per-dimension feedback (handshape, position, motion, speed)
- [ ] Adaptive difficulty based on performance
- [ ] Spaced repetition for review scheduling

### 3.3 Gamification
- [ ] Real XP system with persistence
- [ ] Streak tracking with calendar view
- [ ] Badge unlock logic (milestone-based)
- [ ] Daily challenges with rotating content
- [ ] Leaderboards (optional, privacy-conscious)

---

## Phase 4: Social & Premium

**Goal:** Monetization and community features.

### 4.1 Subscription / Paywall
- [ ] StoreKit 2 (iOS) / Google Play Billing (Android)
- [ ] 3-tier plan: Monthly ($4.99), Yearly ($39.99), Lifetime ($99)
- [ ] 7-day free trial
- [ ] Entitlement checking and feature gating
- [ ] "Free for deaf community" verification flow
- [ ] Restore purchases

### 4.2 Family Sharing
- [ ] Family plan management API
- [ ] Invite flow (link, QR, email)
- [ ] Parental controls with content filtering
- [ ] Shared streak tracking
- [ ] Role management (admin, member, child)

### 4.3 Interpreter Handoff
- [ ] VRS service directory API
- [ ] Deep linking to Sorenson, Convo, ZP InSight apps
- [ ] Context-aware trigger (medical/legal detection)
- [ ] Call initiation or redirect

### 4.4 Community Contribution
- [ ] Video clip recording for training data
- [ ] Consent and privacy framework
- [ ] Daily mission system
- [ ] Contributor badge rewards
- [ ] Secure upload pipeline

---

## Phase 5: Scale

**Goal:** Multi-language support and advanced features.

### 5.1 Additional Languages
- [ ] BSL (British Sign Language) model
- [ ] LSF (Langue des Signes Française) model
- [ ] JSL (Japanese Sign Language) model
- [ ] 10+ additional languages (roadmap mentions 14 total)
- [ ] Language detection (auto-detect sign language variant)

### 5.2 Offline Model Management
- [ ] Model CDN and download manager
- [ ] Background download with progress notifications
- [ ] Model versioning and OTA updates
- [ ] Integrity verification (checksum)
- [ ] Storage management with size optimization

### 5.3 Second Screen / Cast
- [ ] Device discovery (local network)
- [ ] QR code pairing
- [ ] Screen mirroring or extended display
- [ ] TV casting (Chromecast / AirPlay)

### 5.4 Advanced AI
- [ ] Sentence-level recognition (not just individual signs)
- [ ] Context-aware translation
- [ ] Fingerspelling recognition
- [ ] Facial expression analysis (part of sign language grammar)
- [ ] Signing avatars (3D animated sign output)
- [ ] AR overlays for sign learning

---

## Known Limitations

### Architecture
1. **No ViewModel / state management** — All state is local `mutableStateOf` in composables. Will not survive configuration changes on Android.
2. **No dependency injection** — Direct `MockSayvaData` access. Cannot swap implementations for testing.
3. **No repository pattern** — Data access is tightly coupled to UI.
4. **No error handling framework** — No try/catch, no error states wired to real failures.
5. **Single module for all UI** — `:shared` contains everything. Feature modules would improve build times and encapsulation.

### UI
6. **No dark theme** — `SayvaColorScheme` only defines a light scheme. Dark mode toggle in Settings is visual only.
7. **No animations** — No Compose animations, transitions, or motion. The UI is static.
8. **No localization** — All strings are hardcoded in English. No `strings.xml` or Compose resources for i18n.
9. **No responsive layout** — No tablet or landscape adaptations.
10. **No edge-to-edge handling** — No `WindowInsets` management for status bar or navigation bar.

### Testing
11. **No meaningful tests** — Only `assertEquals(3, 1 + 2)` placeholder tests exist.
12. **No UI tests** — No Compose test rules or screenshot tests.
13. **No integration tests** — No end-to-end flow verification.

### Security
14. **No credential storage** — Login/register are visual only.
15. **No data encryption** — No data exists to encrypt yet.
16. **No certificate pinning** — No network calls exist yet.

---

## Missing Features (PM Assessment)

As assessed from a Senior PM perspective, the following features are expected for a production sign language app but are absent:

### Critical Missing
1. **Onboarding personalization** — No user preference collection (preferred sign language, skill level, use case)
2. **Search** — Search bars exist but are non-functional across History, Favorites, SavedConversations
3. **Deep linking** — No URL scheme or App Links support
4. **Push notifications** — NotificationsScreen exists but no push notification infrastructure
5. **App review / rating prompt** — No in-app review trigger

### Important Missing
6. **Offline detection** — No network state monitoring (toast pattern exists in SystemStates)
7. **Data export** — No conversation/history export capability
8. **Help / Support** — "Help centre" menu item exists but navigates nowhere
9. **Terms of Service / Privacy Policy** — Referenced in Register but no content
10. **App update prompt** — No version checking or force-update mechanism

### Nice to Have Missing
11. **Widget** — No home screen widget for quick translate
12. **Watch companion** — No Apple Watch / Wear OS support
13. **Shortcuts / Quick Actions** — No iOS Shortcuts or Android App Shortcuts
14. **Siri / Google Assistant integration** — No voice assistant hooks
15. **Share extension** — No share sheet integration for receiving text to sign

---

## Improvement Recommendations

### Architecture (Priority: High)
1. **Introduce ViewModels** — Extract state management from composables into `ViewModel` classes using `lifecycle-viewmodel-compose` (already a dependency)
2. **Add repository layer** — Abstract data access behind interfaces for testability
3. **Add dependency injection** — Consider Koin (KMP-compatible) for service location
4. **Modularize by feature** — Split `:shared` into feature modules (`:feature:translate`, `:feature:learn`, etc.)

### Code Quality (Priority: High)
5. **Extract inline components** — Promote frequently-used patterns (LabeledField, ToggleRow, GradientCard) to shared composables
6. **Centralize string resources** — Move all hardcoded strings to Compose resources for i18n readiness
7. **Add Compose semantics** — Critical for accessibility; should be done before any public release
8. **Write tests** — At minimum: navigation tests, data model tests, ViewModel tests

### UX (Priority: Medium)
9. **Add loading skeletons** — Replace hardcoded content with shimmer/skeleton loading states
10. **Add empty states** — Pattern exists in SystemStates but not applied to actual screens
11. **Add error recovery** — Wire error state pattern from SystemStates to real error scenarios
12. **Implement dark theme** — Define `darkColorScheme` counterpart in Theme.kt
13. **Add transitions** — Screen transition animations using `AnimatedContent`

### Performance (Priority: Medium)
14. **Optimize icon rendering** — SymbolIcon uses BasicText per icon; consider caching font families
15. **LazyColumn keys** — Add explicit `key` parameters to `LazyColumn` items for stable recomposition
16. **Image/video loading** — Plan for Coil/Glide integration when real media is added

### Security (Priority: High for production)
17. **Add ProGuard/R8 rules** — `isMinifyEnabled = false` in release build
18. **Implement certificate pinning** — When network layer is added
19. **Secure credential storage** — Keychain (iOS) / EncryptedSharedPreferences (Android)
20. **Add content security policy** — For any future WebView content
