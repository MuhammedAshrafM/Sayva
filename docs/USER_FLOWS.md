# User Flows

## Table of Contents

- [Flow Map](#flow-map)
- [1. First Launch](#1-first-launch)
- [2. Returning User Sign-In](#2-returning-user-sign-in)
- [3. Quick Translation](#3-quick-translation)
- [4. Two-Way Conversation](#4-two-way-conversation)
- [5. Low Confidence Recovery](#5-low-confidence-recovery)
- [6. Review Past Translation](#6-review-past-translation)
- [7. Emergency Phrase Speak](#7-emergency-phrase-speak)
- [8. Learn a New Sign](#8-learn-a-new-sign)
- [9. Practice Quiz](#9-practice-quiz)
- [10. Upgrade to Plus](#10-upgrade-to-plus)
- [11. Interpreter Handoff](#11-interpreter-handoff)
- [12. Manage Offline Models](#12-manage-offline-models)
- [13. Contribute Training Data](#13-contribute-training-data)
- [14. Pair Second Screen](#14-pair-second-screen)
- [15. Password Recovery](#15-password-recovery)

---

## Flow Map

```
                    ┌─────────────┐
                    │   Welcome   │
                    └──────┬──────┘
                           │
              ┌────────────┼─────────────┐
              ▼            ▼             ▼
         HowAiWorks    (Skip)        (Skip)
              │            │             │
              ▼            │             │
         TwoWayIntro      │             │
              │            │             │
              ▼            │             │
         Permissions       │             │
              │            │             │
              ▼            ▼             ▼
         ┌─────────────────────────────────┐
         │            Login                │
         │   ┌──────┬──────┬──────┐       │
         │   │Sign  │Guest │Create│       │
         │   │in    │mode  │acct  │       │
         └───┴──┬───┴──┬───┴──┬───┘       │
                │      │      │            │
                │      │      ▼            │
                │      │   Register ───────┤
                │      │                   │
                ▼      ▼                   │
         ┌──────────────────┐              │
         │   HOME SCREEN    │◄─────────────┘
         │  (Bottom Nav)    │
         ├──────┬───────────┤
         │Home  │Translate  │
         │Learn │You        │
         └──────┴───────────┘
```

---

## 1. First Launch

**Actor:** New user opening the app for the first time

**Entry:** App launch → `Screen.Welcome`

**Steps:**
1. User sees Welcome screen with "Talk with your hands" hero
2. Tap "Get started" → HowAiWorks (3-step AI explanation)
3. Tap "Next" → TwoWayIntro (conversation demo)
4. Tap "Almost done" → Permissions (camera, mic, notifications)
5. Tap "Continue" → Login
6. Choose: Sign in / Create account / Guest mode
7. → Home (via `replaceAll` — no back to onboarding)

**Alternate paths:**
- Skip from any onboarding step → Login directly

**Post-conditions:**
- User lands on Home with bottom navigation visible
- Onboarding screens removed from back-stack

---

## 2. Returning User Sign-In

**Actor:** Existing user with account

**Entry:** `Screen.Login`

**Steps:**
1. Enter email and password
2. Tap "Sign in" → Home
3. Alternatively: Tap biometric sign-in → Home

**Alternate paths:**
- Forgot password → ForgotPassword → ResetEmailSent
- Create account → Register → Home
- Guest mode → Home

---

## 3. Quick Translation

**Actor:** User needing immediate sign-to-text translation

**Entry:** Home → "Translate now" card, or Bottom nav → Translate tab

**Steps:**
1. Tap "Translate now" or Translate tab → LiveCamera
2. Camera viewport displays (mock gradient)
3. Detection brackets appear, FPS/latency shown
4. AI recognizes sign → result card appears with translation and confidence
5. Tap "Speak aloud" → TTS reads translation aloud
6. Tap copy → translation copied to clipboard (mock)

**Alternate paths:**
- Low confidence → AiFeedbackLowConfidence screen
- Tap Conversation button → Conversation mode
- Tap History button → History list

**Post-conditions:**
- Translation visible in result card
- TTS playback if requested

---

## 4. Two-Way Conversation

**Actor:** Deaf user communicating with hearing person

**Entry:** LiveCamera → Conversation button, or Home → Conversation quick action

**Steps:**
1. Navigate to Conversation screen
2. "REC" badge and timer visible
3. User signs → sign bubbles appear (left, primary color)
4. Hearing person speaks → voice bubbles appear (right, surface color)
5. Conversation builds as transcript
6. Tap any bubble → TTS speaks that message
7. Tap "Stop & save" → saved to SavedConversations

**Alternate paths:**
- Download transcript
- Share transcript
- Delete conversation

---

## 5. Low Confidence Recovery

**Actor:** User when AI recognition confidence is low

**Entry:** LiveCamera → confidence < threshold → AiFeedbackLowConfidence

**Steps:**
1. Warning chip shows "LOW CONFIDENCE · 42%"
2. Bottom sheet presents 3 alternative suggestions with confidence %
3. First suggestion highlighted
4. User reviews suggestions
5. Choose: "Try again" (re-scan) or "Type instead" (manual input)

**Post-conditions:**
- Both actions return to previous screen via `nav.back()`

---

## 6. Review Past Translation

**Actor:** User reviewing translation history

**Entry:** Home → History quick action, or Profile → History row

**Steps:**
1. Navigate to History screen
2. Browse grouped list (TODAY, YESTERDAY)
3. Optionally: search or apply filter chips
4. Tap entry → HistoryDetail
5. View recognized sign, confidence, language, timestamp
6. Tap speaker icon → TTS reads "spoken as" text
7. Toggle favorite, share, correct, or delete

**Post-conditions:**
- Favorite state toggled (local state only)
- TTS playback if requested

---

## 7. Emergency Phrase Speak

**Actor:** User needing to quickly communicate an emergency phrase

**Entry:** Home → Favorites quick action

**Steps:**
1. Navigate to Favorites screen
2. Locate emergency phrase (e.g., "I need help now")
3. Tap red speak button → TTS immediately reads phrase aloud
4. Optionally: enable Emergency mode toggle

**Post-conditions:**
- Phrase spoken aloud via platform TTS
- Emergency mode visual toggle (UI only)

---

## 8. Learn a New Sign

**Actor:** User studying sign language

**Entry:** Bottom nav → Learn tab

**Steps:**
1. View LearnCategories screen with 6 categories
2. See progress per category (e.g., "8 / 12 signs")
3. Tap category → LessonPlayer
4. Watch video demonstration (placeholder)
5. Read sign description and tags
6. Tap "Try it" → Practice screen

**Alternate paths:**
- Tap "Replay" to re-watch
- Use slow-motion playback

---

## 9. Practice Quiz

**Actor:** User practicing after a lesson

**Entry:** LessonPlayer → "Try it" button

**Steps:**
1. Enter Practice screen with dark camera-style background
2. See prompt: "SIGN THE PHRASE" with target text
3. Perform the sign
4. Tap "I signed it"
5. Receive feedback:
   - Handshape ✓, Position ✓, Motion feedback
   - Match percentage (e.g., 84%)
   - Advice text
6. Choose "Retry" or "Accept & next"
7. After 5 questions → Progress screen

**Post-conditions:**
- XP awarded (mock)
- Progress screen shows updated stats

---

## 10. Upgrade to Plus

**Actor:** Free-tier user considering premium

**Entry:** Profile → "Upgrade to Plus" row

**Steps:**
1. Navigate to Paywall screen
2. See "SAYVA PLUS" badge and feature list
3. Review 3 plans (Monthly, Yearly, Lifetime)
4. Yearly pre-selected with "SAVE 33%" badge
5. Tap plan card to select
6. Tap "Start 7-day free trial"

**Alternate paths:**
- "Free for deaf community" link
- Restore purchases

**Post-conditions:**
- No real payment processing (UI only)

---

## 11. Interpreter Handoff

**Actor:** User in a medical or legal context needing professional interpreter

**Entry:** Triggered by detected context (mock) or manual navigation

**Steps:**
1. See red warning: "Medical / legal context detected"
2. Review interpreter options:
   - Sorenson VRS (featured, "Free with VRS")
   - Convo Relay
   - ZP InSight (BSL)
3. Tap "Call now" on featured option
4. Or tap "Keep using Sayva" to return

**Post-conditions:**
- No real VRS call initiated (mock)

---

## 12. Manage Offline Models

**Actor:** User downloading language packs for offline use

**Entry:** Profile → Offline models, or Settings → Offline models

**Steps:**
1. Navigate to OfflineModels screen
2. View storage visualization (donut chart)
3. See downloaded packs (ASL active, BSL)
4. See available packs (LSF, JSL beta)
5. Tap download on available pack → added to downloaded list (mock toggle)
6. Optionally: clear cache, remove non-active packs

**Post-conditions:**
- `downloadedIds` set updated (session-only state)

---

## 13. Contribute Training Data

**Actor:** User helping improve AI accuracy

**Entry:** Profile → "Contribute to AI" row

**Steps:**
1. Navigate to Contribute screen
2. View impact stats ("38 clips · 2 badges")
3. Enable "Share my sign samples" toggle
4. See today's mission: "Record 5 greetings" (3/5 done)
5. Tap "Record next" to contribute
6. View upload progress

**Post-conditions:**
- Toggle state updated (session-only)
- No real recording or upload

---

## 14. Pair Second Screen

**Actor:** User connecting a secondary display

**Entry:** Profile or Settings (navigation path exists)

**Steps:**
1. Navigate to PairSecondScreen
2. View mock QR code
3. Note 4-digit pairing code (7391)
4. See connected device: "Reception iPad"
5. Optionally: "Cast to TV"

**Post-conditions:**
- No real pairing (mock)

---

## 15. Password Recovery

**Actor:** User who forgot their password

**Entry:** Login → "Forgot password?" link

**Steps:**
1. Tap "Forgot password?" → ForgotPassword
2. Enter email address
3. Tap "Send reset link" → ResetEmailSent
4. See confirmation: "Check your inbox"
5. Wait for resend timer (45s, hardcoded)
6. Tap "Open email app" (mock action)
7. Pro-tip suggests guest mode as alternative

**Post-conditions:**
- No real email sent
