# Features

## Table of Contents

- [Feature Summary](#feature-summary)
- [1. Onboarding](#1-onboarding)
- [2. Authentication](#2-authentication)
- [3. Home Dashboard](#3-home-dashboard)
- [4. Live Camera Translation](#4-live-camera-translation)
- [5. Conversation Mode](#5-conversation-mode)
- [6. AI Feedback (Low Confidence)](#6-ai-feedback-low-confidence)
- [7. Translation History](#7-translation-history)
- [8. Favorites](#8-favorites)
- [9. Saved Conversations](#9-saved-conversations)
- [10. Learning System](#10-learning-system)
- [11. Practice & Quizzes](#11-practice--quizzes)
- [12. Progress Tracking](#12-progress-tracking)
- [13. User Profile](#13-user-profile)
- [14. Settings](#14-settings)
- [15. Accessibility](#15-accessibility)
- [16. Notifications](#16-notifications)
- [17. Paywall / Subscription](#17-paywall--subscription)
- [18. Family Sharing](#18-family-sharing)
- [19. Offline Models](#19-offline-models)
- [20. First Launch Model Download](#20-first-launch-model-download)
- [21. Contribute to AI](#21-contribute-to-ai)
- [22. Interpreter Handoff](#22-interpreter-handoff)
- [23. Crash Reporting](#23-crash-reporting)
- [24. Second Screen Pairing](#24-second-screen-pairing)
- [25. System States (Reference)](#25-system-states-reference)
- [26. Text-to-Speech](#26-text-to-speech)

---

## Feature Summary

| # | Feature | Screens | Status |
|---|---|---|---|
| 1 | Onboarding | Welcome, HowAiWorks, TwoWayIntro, Permissions | UI complete, no real permissions |
| 2 | Authentication | Login, Register, ForgotPassword, ResetEmailSent | UI complete, no backend |
| 3 | Home Dashboard | Home | UI complete, mock data |
| 4 | Live Camera Translation | LiveCamera | UI complete, no real camera/AI |
| 5 | Conversation Mode | Conversation | UI complete, mock transcript |
| 6 | AI Feedback | AiFeedbackLowConfidence | UI complete, mock suggestions |
| 7 | Translation History | History, HistoryDetail | UI complete, mock data |
| 8 | Favorites | Favorites | UI complete, mock phrases |
| 9 | Saved Conversations | SavedConversations | UI complete, mock data |
| 10 | Learning System | LearnCategories, LessonPlayer | UI complete, mock content |
| 11 | Practice & Quizzes | Practice | UI complete, mock quiz |
| 12 | Progress Tracking | Progress | UI complete, mock stats |
| 13 | User Profile | Profile | UI complete, mock user |
| 14 | Settings | Settings | UI complete, toggles non-functional |
| 15 | Accessibility | Accessibility | UI complete, toggles non-functional |
| 16 | Notifications | Notifications | UI complete, mock items |
| 17 | Paywall | Paywall | UI complete, no payment SDK |
| 18 | Family Sharing | Family | UI complete, mock members |
| 19 | Offline Models | OfflineModels | UI complete, mock packs |
| 20 | First Launch Download | FirstLaunchModelDownload | UI complete, mock progress |
| 21 | Contribute to AI | Contribute | UI complete, mock missions |
| 22 | Interpreter Handoff | InterpreterHandoff | UI complete, mock services |
| 23 | Crash Reporting | CrashReport | UI complete, no crash SDK |
| 24 | Second Screen Pairing | PairSecondScreen | UI complete, mock QR/code |
| 25 | System States | SystemStates | Reference/demo screen |
| 26 | Text-to-Speech | (cross-cutting) | **Implemented** via expect/actual |

---

## 1. Onboarding

### Purpose
Introduce new users to Sayva's AI-powered sign language translation capabilities and collect required permissions.

### Screens
- **WelcomeScreen** — Hero landing with brand message
- **HowAiWorksScreen** — 3-step AI pipeline explanation
- **TwoWayIntroScreen** — Live conversation demo mockup
- **PermissionsScreen** — Camera, mic, notification permission requests

### User Flow
```
Welcome → HowAiWorks → TwoWayIntro → Permissions → Login
       └── Skip ──────────────────────────────────→ Login
```

### UI Components
- Gradient hero box with `sign_language` icon (Welcome)
- 3 StepCards with icons: camera (60fps), auto_awesome (24ms AI), volume_up (voice) (HowAiWorks)
- Mock chat bubbles with sign/voice alignment (TwoWayIntro)
- PermissionCard rows with granted/optional states (Permissions)
- DotIndicator (3 dots) on each onboarding step
- Privacy badge with lock icon

### States
- **Default:** Sequential step progression
- **Skip:** User can skip directly to Login from any step

### Business Rules
- Camera permission is marked "Required" with a green "Granted" badge
- Microphone and Notifications are marked "Optional"
- Privacy promise: "Your data never leaves your device"
- Skip is always available

### Navigation
- Forward: `navigate()` to next screen
- Skip: `replaceAll(Screen.Login)` — clears onboarding from back-stack
- Back: standard `back()` to previous step

---

## 2. Authentication

### Purpose
Account creation, sign-in, and password recovery flows.

### Screens
- **LoginScreen** — Email/password sign-in
- **RegisterScreen** — Account creation
- **ForgotPasswordScreen** — Password reset request
- **ResetEmailSentScreen** — Confirmation of sent reset email

### User Flow
```
Login ──→ Home (sign in / guest mode)
  │
  ├──→ Register ──→ Home
  ├──→ ForgotPassword ──→ ResetEmailSent
  └──→ Guest mode ──→ Home (replaceAll)
```

### UI Components
- LabeledField (email, password, name) with focus indicator
- Password strength bar (4 colored segments: red → orange → yellow → green)
- Biometric sign-in button (fingerprint icon)
- Sign language selector dropdown (default: ASL)
- Terms & privacy checkbox
- Resend countdown timer (ResetEmailSent)
- Pro-tip card about guest mode

### States
- **Login:** Email field shown with "focused" border state
- **Register:** Password strength dynamically rendered (hardcoded "Strong" in mock)
- **ResetEmailSent:** Countdown timer display (hardcoded "45 seconds")

### Business Rules
- Guest mode grants full app access via `replaceAll(Screen.Home)`
- Sign-in and registration both navigate to Home
- Password requirements shown but not validated (mock)
- Terms checkbox present but not enforced

---

## 3. Home Dashboard

### Purpose
Central hub providing quick access to all major features with personalized greeting and daily engagement.

### Screen
- **HomeScreen**

### UI Components
- User greeting with gradient avatar circle ("A" initial)
- Notification bell with red unread dot
- Search bar with microphone icon (mock — no search logic)
- "Translate now" gradient card (Primary40 → Primary60) with camera icon
- 6-item quick action grid:
  - Conversation → `Screen.Conversation`
  - History → `Screen.History`
  - Favorites → `Screen.Favorites`
  - Learn → `Screen.LearnCategories`
  - Saved chats → `Screen.SavedConversations`
  - Settings → `Screen.Settings`
- Daily challenge card with streak counter, XP earned, progress bar

### States
- **Default:** All elements rendered with mock data
- **No empty/loading/error states implemented**

### Business Rules
- Greeting shows "Good morning, Alex" (hardcoded)
- Daily challenge: "Learn 3 new food signs" with 1/3 progress
- Streak: "12-day streak" with fire emoji
- Quick action grid uses 2-column layout

---

## 4. Live Camera Translation

### Purpose
Real-time sign language recognition through the device camera with AI-powered translation.

### Screen
- **LiveCameraScreen**

### UI Components
- Dark radial gradient viewport (camera mockup)
- Detection brackets (corner L-shapes indicating detection zone)
- Top chrome: close, language selector pill ("ASL · EN"), flash, camera switch
- FPS/latency chips: "62 FPS" and "24 ms"
- Hand detection status label
- Translation result card:
  - Recognized sign: "Thank you"
  - Confidence: 96% with gradient progress bar
  - "Speak aloud" button (calls `speakText()` — **functional**)
  - Copy button
- Bottom toolbar: Conversation mode, pause/play, History
- Floating camera mini-preview with "LIVE" badge

### States
- **Active:** Shows detection brackets, FPS counter, recognition result
- **No actual camera feed** — viewport is a gradient placeholder
- **No real AI inference** — confidence and sign are hardcoded

### Business Rules
- Confidence displayed as percentage with color-coded bar
- "Speak aloud" triggers platform TTS (the only functional feature)
- Language shown as "ASL · EN" (American Sign Language to English)
- Hardcoded values: sign = "Thank you", confidence = 96

### Dependencies
- `speakText()` (expect/actual — functional)
- Camera API (not implemented)
- ML model (not implemented)

---

## 5. Conversation Mode

### Purpose
Two-way real-time conversation between a sign language user and a hearing person, with transcript history.

### Screen
- **ConversationScreen**

### UI Components
- Top bar with "REC" badge, timer ("04:32"), partner name
- Date divider ("Today · Jun 2025")
- Transcript list:
  - Sign bubbles (Primary-tinted, left-aligned): "Hello, I'd like to order please"
  - Voice bubbles (Surface-tinted, right-aligned): "Of course! What would you like?"
  - Metadata: "Signed · 2:31 PM" / "Voice · 2:32 PM"
- Typing indicator bubble with animated dots
- Floating camera mini-preview (bottom-right corner)
- Bottom toolbar: download, share, stop & save, delete

### States
- **Recording:** "REC" badge visible, timer running (hardcoded)
- **Typing:** Animated dot bubble shown

### Business Rules
- Transcript is hardcoded (5 alternating sign/voice messages)
- "Stop & save" navigates to SavedConversations
- Each bubble is clickable (implied `speakText()` integration)

---

## 6. AI Feedback (Low Confidence)

### Purpose
When the AI model has low confidence in a recognition, present alternative suggestions and recovery options.

### Screen
- **AiFeedbackLowConfidenceScreen**

### UI Components
- Dark gradient background
- Warning chip: "LOW CONFIDENCE · 42%"
- Bottom sheet with suggestion list:
  - Each row: icon, sign name, description, confidence percentage
  - First suggestion highlighted with Primary40 border
- Tip card: "Try better lighting and keep hands visible"
- Action buttons: "Type instead", "Try again"

### States
- **Low confidence detected:** Shows 3 suggestions from `MockSayvaData.lowConfidenceSuggestions`

### Business Rules
- Suggestions sourced from `MockSayvaData` (3 items with varying confidence)
- 42% confidence threshold triggers this screen (hardcoded)
- "Type instead" and "Try again" both call `nav.back()`

---

## 7. Translation History

### Purpose
Browse, search, and filter past translation entries.

### Screens
- **HistoryScreen** — List view with search and filters
- **HistoryDetailScreen** — Detail view of a single translation

### UI Components

**HistoryScreen:**
- Search bar with microphone
- Horizontal filter chips: All, Today, Favs, ASL, High confidence
- Grouped list by day ("TODAY", "YESTERDAY")
- History rows with icon badge, title, metadata (time, confidence, language)
- Star indicator for favorites
- FAB → LiveCamera

**HistoryDetailScreen:**
- Video replay placeholder with play button and progress bar
- Recognized sign title
- Metadata chips: confidence %, language, time
- "Spoken as" section with TTS playback button
- Action tiles: Delete, Favorite (toggleable), Correct, Share

### States
- **Default:** 5 history items from MockSayvaData
- **Filter active:** `selectedFilter` state variable (UI only, no actual filtering)
- **Favorite toggle:** `isFavorite` state in HistoryDetail

### Business Rules
- History entries grouped by `day` field
- Confidence color coding: green (high), amber (medium), red (low)
- Low-confidence entries show warning icon
- Conversation entries show forum icon with message count and duration

---

## 8. Favorites

### Purpose
Quick-access phrase board with emergency mode for critical communication.

### Screen
- **FavoritesScreen**

### UI Components
- Filter pills: "All · 24", "Greetings", "Medical", "Family"
- 2-column grid of phrase cards with gradient backgrounds and icons
- Each card: category label, phrase text, speak button
- "Add phrase" tile
- "AI suggestions" tile
- Emergency mode toggle bar at bottom

### States
- **Default:** 4 favorite phrases from MockSayvaData
- **Emergency mode:** Toggle modifies UI (visual only)
- **Filter selection:** `selectedFilter` state variable

### Business Rules
- Emergency phrases (e.g., "I need help now") have `isEmergency = true`
- Emergency speak button uses red/Secondary50 instead of white
- Speak button calls `speakText()` (**functional**)
- Each phrase has a unique gradient and icon

---

## 9. Saved Conversations

### Purpose
Access previously saved two-way conversation transcripts.

### Screen
- **SavedConversationsScreen**

### UI Components
- Search bar
- Conversation cards with:
  - Dual overlapping avatar circles
  - Title, time label, preview text (italic)
  - Message count and category badges
  - Star indicator for favorites
  - Highlighted cards have gradient accent border
- "New chat" FAB

### States
- **Default:** 3 saved conversations from MockSayvaData

### Business Rules
- Featured conversation has `highlighted = true` (gradient border)
- Category badges show context (e.g., "Medical" with colored background)
- FAB navigates to Conversation screen

---

## 10. Learning System

### Purpose
Structured sign language lessons organized by category with progress tracking.

### Screens
- **LearnCategoriesScreen** — Category grid with progress
- **LessonPlayerScreen** — Individual lesson viewer

### UI Components

**LearnCategoriesScreen:**
- Streak badge with fire emoji
- Daily challenge card (gradient, premium icon)
- 2-column category grid
- CategoryCard: icon, name, progress ("8 / 12 signs"), colored progress bar, checkmark if complete

**LessonPlayerScreen:**
- Video player placeholder (gradient background with slow-mo icon)
- "SLOW-MO" badge
- Playback controls: play/pause, progress slider, 0.5x speed
- Lesson title, description, tag pills ("One hand", "Forward motion")
- Bottom actions: "Replay", "Try it" (→ Practice)

### States
- **Default:** 6 categories from MockSayvaData
- **Lesson loaded:** 1 lesson ("Hello") from MockSayvaData

### Business Rules
- 6 categories: Greetings, Numbers, Family, Food & Drink, Medical, Emotions
- Each category tracks learned/total ratio
- Progress bar color matches category color
- "Try it" navigates to Practice screen

---

## 11. Practice & Quizzes

### Purpose
Interactive quiz flow where users practice signing and receive AI feedback.

### Screen
- **PracticeScreen**

### UI Components
- Dark radial gradient background
- Progress counter ("Q 1 of 5") with progress bar
- XP badge ("+10 XP")
- Prompt card: "SIGN THE PHRASE" with quoted target
- Pre-answer: "Skip" and "I signed it" buttons
- Post-answer feedback:
  - Feedback chips: Handshape ✓, Position ✓, "Slow it down" (warning)
  - Result card: 84% match with advice text
  - "Retry" and "Accept & next" buttons

### States
- **Unanswered:** Shows skip and "I signed it" buttons
- **Answered:** Shows feedback chips and result card
- **Quiz progress:** `index` tracks current question, `answered` tracks state

### Business Rules
- 5 questions from MockSayvaData
- Confidence threshold: 84% (hardcoded)
- Completing all questions navigates to Progress screen
- XP awarded per question (mock)

---

## 12. Progress Tracking

### Purpose
Dashboard showing learning streaks, stats, weekly activity, and earned badges.

### Screen
- **ProgressScreen**

### UI Components
- Streak hero card (gradient): "12 DAY STREAK" with personal best
- Stat tiles grid: Total XP (847), Signs learned (38), Lessons (12), Badges (3)
- Weekly bar chart with day labels (M–S)
- Badges horizontal carousel (4 badges, 1 locked)
- Share button

### States
- **Default:** Stats from `MockSayvaData.progressStats`

### Business Rules
- Today's bar highlighted in WarningColor (amber)
- Peak day bar highlighted in Primary40 (indigo)
- Locked badges rendered semi-transparent
- Personal best streak displayed alongside current

---

## 13. User Profile

### Purpose
User account overview with access to all account-related features.

### Screen
- **ProfileScreen**

### UI Components
- "You" headline with notification bell
- Gradient avatar with edit overlay badge
- User info: "Alex Hale", email, "Sayva Plus" badge
- Stat tiles: 12-day streak, 2.4k translations, 38 signs
- 9 menu rows:
  1. History → `Screen.History`
  2. Offline models → `Screen.OfflineModels`
  3. Contribute to AI (NEW badge) → `Screen.Contribute`
  4. Family → `Screen.Family`
  5. Accessibility → `Screen.Accessibility`
  6. Notifications → `Screen.Notifications`
  7. Upgrade to Plus → `Screen.Paywall`
  8. Settings → `Screen.Settings`
  9. Help centre → (no navigation)
- Version footer: "Sayva v1.0.0 (42)" with sign out link

### Business Rules
- Each menu row has a unique colored icon background
- "NEW" badge on Contribute row
- "Upgrade to Plus" row uses WarningColor icon
- Sign out: `replaceAll(Screen.Welcome)`

---

## 14. Settings

### Purpose
App-wide configuration including display, speech, camera, and diagnostics.

### Screen
- **SettingsScreen**

### UI Components
- **DISPLAY:** Dark mode toggle (Light/Auto/Dark), font size slider, high contrast switch
- **SPEECH & SOUND:** Voice selector ("Ava" American English), speech speed slider
- **CAMERA & AI:** Camera quality ("1080p HD"), offline mode switch, link to Offline Models, link to Accessibility
- **DIAGNOSTICS:** System states link, Report a problem link
- Clear cache button ("48 MB"), Reset to defaults button

### States
- `darkMode`, `highContrast`, `offlineMode` — `mutableStateOf` toggles (visual only, no theme switching)

### Business Rules
- Dark mode toggle renders 3 options but does not change theme
- Font size slider renders but does not change text size
- Settings are not persisted between sessions

---

## 15. Accessibility

### Purpose
Comprehensive accessibility settings designed for Deaf and hard-of-hearing users.

### Screen
- **AccessibilityScreen**

### UI Components
- Easy mode hero card with gradient and large toggle
- Toggle rows:
  - Larger text (18 pt minimum)
  - High contrast
  - Color blind mode (Off / Deuteranopia / Protanopia / Tritanopia)
  - Left-handed mode
  - Haptic feedback slider (0–100%)
  - Reduce motion
  - Screen reader hints (TalkBack / BrailleBack)

### States
- 7 `mutableStateOf` toggle variables (visual only)
- Color blind mode uses segmented button group

### Business Rules
- All toggles are UI-only; no accessibility changes are applied
- Easy mode description: "Simplify interface, enlarge targets, slow animations"
- Roadmap text mentions future: "Signing avatars, AR overlays, vibration patterns"

---

## 16. Notifications

### Purpose
In-app notification center with visual-first design (no audio reliance).

### Screen
- **NotificationsScreen**

### UI Components
- Grouped list: "TODAY", "THIS WEEK"
- Notification rows: red dot (unread), icon with colored background, title, body, time, action label
- "Mark all" button in top bar
- Footer note: "Flash pulse + haptic buzz, never just audio"

### States
- **Default:** 5 notifications from MockSayvaData

### Business Rules
- Highlighted notifications have alpha-tinted icon background
- Visual-first: designed for users who cannot hear audio alerts
- Action labels (e.g., "View") present but non-functional

---

## 17. Paywall / Subscription

### Purpose
Subscription upgrade screen with pricing tiers and feature comparison.

### Screen
- **PaywallScreen**

### UI Components
- "SAYVA PLUS" premium badge (gold gradient)
- 3 plan cards:
  - Monthly: $4.99/month
  - Yearly: $39.99/year (SAVE 33% badge, default selected)
  - Lifetime: $99 one-time (FOREVER badge)
- Feature checklist with checkmarks
- "Start 7-day free trial" CTA button
- "Free for deaf community" link
- Restore purchases link

### States
- `selectedPlan` — defaults to "Yearly"
- Selected plan card has gradient background

### Business Rules
- Yearly plan pre-selected as default
- "Free for deaf community" explicitly linked (social mission)
- No payment SDK integrated; UI only

---

## 18. Family Sharing

### Purpose
Family plan management with member roles and parental controls.

### Screen
- **FamilyScreen**

### UI Components
- Family hero banner (gradient): "THE HALE FAMILY", "4 of 6 members", "28-day shared streak"
- Member list with gradient avatars, names, badges (ADMIN, KID)
- Invite card: "Invite 2 more" via link, QR, or email
- Parental controls toggle bar (dark, bottom-anchored)

### States
- **Default:** 4 family members from MockSayvaData
- Parental controls shown as toggled ON for "Ella" (the KID member)

### Business Rules
- Maximum 6 family members
- Roles: owner (no badge), ADMIN, KID
- Parental controls: "Content filter active for Ella"
- Invite methods: Share link, QR code, email

---

## 19. Offline Models

### Purpose
Download and manage on-device sign language models for offline use.

### Screen
- **OfflineModelsScreen**

### UI Components
- Storage donut chart visualization (418 MB total)
- Legend: ASL (182 MB), BSL (108 MB), Cache (72 MB), Free (56 MB)
- Downloaded packs list: ASL (ACTIVE pill), BSL (delete option)
- Available packs list: LSF (download), JSL (BETA pill, download)
- Clear cache button
- 14-language roadmap description

### States
- `downloadedIds` — set tracking which packs are "downloaded" (mock toggle)

### Business Rules
- ASL is the default active language, cannot be removed
- BSL shown as downloaded but removable
- Beta languages marked with BETA pill
- Storage values are hardcoded (not from device)
- Roadmap mentions 14 languages planned

---

## 20. First Launch Model Download

### Purpose
Guide users through initial AI model download with progress tracking.

### Screen
- **FirstLaunchModelDownloadScreen**

### UI Components
- Sayva logo and branding
- Download visualization (cloud → device with dot animation)
- Progress card: model name, version, gradient progress bar, MB downloaded, ETA, Wi-Fi status
- Step tracker:
  1. Account synced ✓ (green)
  2. Downloading model (active, indigo)
  3. Verifying integrity (pending)
  4. Ready to translate (pending, rocket icon)
- Battery saver tip
- Pause and "Notify when ready" buttons

### States
- `progress` — 0.62f (62%, hardcoded)

### Business Rules
- Model: "ASL-v3.2-quantised", 96 MB
- Progress frozen at 62% (no real download)
- "Notify when ready" → `replaceAll(Screen.Home)`

---

## 21. Contribute to AI

### Purpose
Crowdsource sign language video clips to improve AI model accuracy.

### Screen
- **ContributeScreen**

### UI Components
- Impact card (gradient): "38 clips recorded", "2 badges earned"
- Toggle: "Share my sign samples"
- Today's mission: "Record 5 greetings" (3/5 done, ~30s remaining)
- Progress bar and "Record next" button
- Upload progress indicator
- Privacy assurance card (green)
- Reward loop description

### States
- `contributingEnabled` — toggle state
- `uploadProgress` — mock progress value

### Business Rules
- Contributions are opt-in via toggle
- Daily missions provide gamified motivation
- Privacy assurance: data handled responsibly
- All data is mock; no real recording or upload

---

## 22. Interpreter Handoff

### Purpose
Seamlessly hand off to a professional interpreter when AI confidence is insufficient for critical contexts (medical, legal).

### Screen
- **InterpreterHandoffScreen**

### UI Components
- Dark gradient background
- Red warning banner: "Medical / legal context detected"
- Support agent icon with heading
- Interpreter option cards:
  - Sorenson VRS (featured): wait time, "Free with VRS", "Call now" button
  - Convo Relay: details, chevron
  - ZP InSight (BSL): details, chevron
- Green info banner: "VRS calls are free for deaf and hard-of-hearing users"
- "Keep using Sayva" and "More options" buttons

### States
- **Default:** 3 interpreter options from MockSayvaData

### Business Rules
- Featured service (Sorenson VRS) gets expanded card with CTA
- Medical/legal context detection implied (not implemented)
- VRS calls explicitly noted as free for Deaf users
- All actions call `nav.back()` (no real calling integration)

---

## 23. Crash Reporting

### Purpose
User-controlled crash report submission with privacy transparency.

### Screen
- **CrashReportScreen**

### UI Components
- Error icon with warning overlay badge
- Error title and description
- Toggle rows for report data:
  - Error logs (4 KB) — default ON
  - Device info (Pixel 8, Android 15, 8 GB RAM) — default ON
  - Screenshot (faces auto-blurred) — default ON
- Free-text field: "What were you doing?"
- Privacy card (green): "Verified by Sayva Privacy"
- "Not now" and "Send report" buttons

### States
- 3 `mutableStateOf` toggles for each data category

### Business Rules
- All toggles default to ON (user can opt out of each)
- Face auto-blurring mentioned for screenshot data
- Both buttons navigate to Home via `replaceAll(Screen.Home)`
- No real crash reporting SDK

---

## 24. Second Screen Pairing

### Purpose
Pair a secondary display (tablet, TV) for mirrored or extended translation output.

### Screen
- **PairSecondScreenContent**

### UI Components
- Mock QR code (200×200 grid pattern with center logo)
- 4-digit pairing code display (hardcoded "7 3 9 1")
- Connected device card: "Reception iPad" with green status dot
- "Cast to TV" button (dark)

### States
- **Default:** Shows one connected device (mock)

### Business Rules
- QR code is a mock grid pattern, not a real QR code
- Pairing code is hardcoded
- Device connection is simulated

---

## 25. System States (Reference)

### Purpose
Internal reference screen demonstrating the app's four UI state patterns.

### Screen
- **SystemStatesScreen**

### Demonstrated States
1. **Empty** — "Nothing here yet" with illustration and CTA
2. **Loading** — "Loading AI model" with progress bar (78%)
3. **Error** — "Camera blocked" with error styling and recovery buttons
4. **Success** — "Lesson complete!" with celebration and next-lesson CTA

### Toast Examples
- **Saved toast** — dark background, check icon, "UNDO" action
- **Offline toast** — dark background, cloud_off icon, "using cached model"

---

## 26. Text-to-Speech

### Purpose
Convert translated text to spoken audio output.

### Implementation Status
**Fully implemented** via `expect`/`actual` pattern.

### Platforms
- **Android:** `android.speech.tts.TextToSpeech` with `QUEUE_FLUSH` mode
- **iOS:** `AVFoundation.AVSpeechSynthesizer` with `AVSpeechUtterance`

### Usage Points
- LiveCameraScreen: "Speak aloud" button
- FavoritesScreen: Speak button on each phrase card
- HistoryDetailScreen: "Spoken as" playback
- ConversationScreen: Tap-to-speak on transcript bubbles
