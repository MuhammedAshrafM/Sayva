# AI Pipeline

## Table of Contents

- [Overview](#overview)
- [Pipeline Architecture](#pipeline-architecture)
- [Stage 1: Camera Input](#stage-1-camera-input)
- [Stage 2: Hand Detection](#stage-2-hand-detection)
- [Stage 3: Sign Recognition](#stage-3-sign-recognition)
- [Stage 4: Text Output](#stage-4-text-output)
- [Stage 5: Speech Synthesis](#stage-5-speech-synthesis)
- [Confidence Handling](#confidence-handling)
- [Offline Inference](#offline-inference)
- [Model Management](#model-management)
- [Training Data Collection](#training-data-collection)
- [Implementation Status](#implementation-status)

---

## Overview

Sayva's AI pipeline is designed for **on-device sign language recognition** — translating live camera input of hand gestures into text and speech. The pipeline is described across multiple screens in the app UI but is **not yet implemented**. All inference results, confidence scores, and detection states are hardcoded mock values.

This document describes the pipeline as designed (based on UI specifications), distinguishing what is implemented from what is planned.

---

## Pipeline Architecture

As presented in `HowAiWorksScreen`, the pipeline has three stages:

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Camera     │────▶│   AI Model   │────▶│    Voice     │
│   60 fps     │     │   24 ms      │     │   Output     │
│              │     │              │     │              │
│  Captures    │     │  Processes   │     │  Speaks      │
│  your signs  │     │  in real     │     │  aloud in    │
│  at 60       │     │  time with   │     │  natural     │
│  frames per  │     │  24ms        │     │  voice       │
│  second      │     │  latency     │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
```

**Privacy model:** "All processing happens on your device. No video is sent to servers." (from HowAiWorksScreen)

---

## Stage 1: Camera Input

**Status:** Not implemented (UI mockup only)

**Design specifications (from LiveCameraScreen):**
- Frame rate: 60 FPS (displayed as chip overlay)
- Camera controls: front/back toggle, flash, language selector
- Detection zone: visual brackets indicating the recognition area
- Mini-preview: floating camera window with "LIVE" badge

**UI elements present:**
- Dark radial gradient placeholder (simulates camera viewport)
- Detection bracket corner decorations
- FPS counter chip: "62 FPS"
- Camera switch button (icon only, no functionality)
- Flash toggle button (icon only, no functionality)

**What needs to be built:**
- CameraX (Android) / AVCaptureSession (iOS) integration via expect/actual
- Frame buffer management
- Camera permission handling (declared in PermissionsScreen UI but not in AndroidManifest)

---

## Stage 2: Hand Detection

**Status:** Not implemented (UI mockup only)

**Design specifications (from LiveCameraScreen):**
- Hand detection label shown when hands are in frame
- Detection brackets highlight the recognition zone
- Processing latency: 24ms per frame (displayed as chip)

**What needs to be built:**
- Hand landmark detection (MediaPipe Hands or equivalent)
- Bounding box calculation for detection brackets
- Frame preprocessing (resize, normalize)
- Background thread inference scheduling

---

## Stage 3: Sign Recognition

**Status:** Not implemented (hardcoded values)

**Design specifications (from LiveCameraScreen, AiFeedbackLowConfidenceScreen):**
- Recognized sign displayed as text: "Thank you" (hardcoded)
- Confidence percentage: 96% (hardcoded)
- Language context: "ASL · EN" (hardcoded)
- Low-confidence threshold triggers suggestion screen

**Mock values in code:**
```kotlin
// LiveCameraScreen.kt
val recognizedSign = "Thank you"
val confidence = 96
```

**Low-confidence suggestions (from MockSayvaData):**
- "Thank you" — 72%
- "Please" — 58%
- "Sorry" — 41%

**What needs to be built:**
- Sign classification model (likely TFLite/CoreML)
- Vocabulary mapping (gesture → sign name)
- Confidence score calculation
- Top-N suggestion generation
- Temporal smoothing (avoid flickering between frames)

---

## Stage 4: Text Output

**Status:** Partially implemented (display layer works, no real input)

**Design specifications:**
- Translation result card with sign name and confidence bar
- Copy-to-clipboard button
- Conversation transcript with alternating sign/voice bubbles

**What works:** UI components correctly display hardcoded text and render confidence bars.

**What needs to be built:**
- Pipeline connection from recognition to UI state
- Clipboard integration
- Conversation transcript accumulation
- Text grammar correction / natural language post-processing

---

## Stage 5: Speech Synthesis

**Status:** Implemented

**Implementation:** `speakText()` via `expect`/`actual` pattern.

| Platform | Engine | Details |
|---|---|---|
| Android | `android.speech.tts.TextToSpeech` | `QUEUE_FLUSH`, `Locale.US` |
| iOS | `AVSpeechSynthesizer` | Default voice and rate |

This is the only stage of the pipeline that is fully functional.

---

## Confidence Handling

### Thresholds (inferred from UI)

| Level | Confidence | Color | Behavior |
|---|---|---|---|
| High | ≥ 90% | `Tertiary50` (green) | Show result directly |
| Medium | 60–89% | `WarningColor` (amber) | Show result with caution |
| Low | < 60% | `ErrorColor` (red) | Trigger AiFeedbackLowConfidence screen |

### Low Confidence Flow

```
LiveCamera (low confidence detected)
    │
    ▼
AiFeedbackLowConfidence
    ├── Shows top 3 suggestions with confidence %
    ├── First suggestion highlighted
    ├── Tip: "better lighting and keep hands visible"
    │
    ├── "Try again" → back to LiveCamera
    └── "Type instead" → back to LiveCamera
```

### Practice Evaluation

The PracticeScreen shows a different confidence model for learning:

- Match percentage: 84% (hardcoded)
- Feedback dimensions: Handshape ✓, Position ✓, Motion (needs work)
- Advice: "Great form! Try speeding up the motion slightly."

---

## Offline Inference

### Design (from OfflineModelsScreen, SettingsScreen)

The app is designed for fully offline operation with on-device models:

**Language packs (from MockSayvaData):**

| Pack | Size | Status |
|---|---|---|
| ASL (American Sign Language) | 182 MB | Active (default) |
| BSL (British Sign Language) | 108 MB | Downloaded |
| LSF (Langue des Signes Française) | 95 MB | Available |
| JSL (Japanese Sign Language) | 87 MB | Beta, available |

**Total storage:** 418 MB (mock visualization in OfflineModelsScreen)

**Offline mode toggle:** Present in SettingsScreen under "CAMERA & AI" section. Toggle exists but has no backend.

**Offline toast:** SystemStatesScreen shows "You're offline · using cached model" toast pattern.

---

## Model Management

### First Launch Download (from FirstLaunchModelDownloadScreen)

```
Step 1: Account synced           ✓ (complete)
Step 2: Downloading model        ● (active — 62%)
Step 3: Verifying integrity      ○ (pending)
Step 4: Ready to translate       ○ (pending)
```

**Model metadata (hardcoded):**
- Name: "ASL-v3.2-quantised"
- Size: 96 MB
- Progress: 62% (frozen)
- ETA: "~2 min remaining"
- Connection: Wi-Fi

**What needs to be built:**
- Model hosting and CDN
- Download manager with pause/resume
- Integrity verification (checksum)
- Model versioning and updates
- Storage management
- Background download with notification

### Storage Management

From OfflineModelsScreen:
- Donut chart visualization of storage usage
- Per-pack size display
- Cache clearing (72 MB cache, hardcoded)
- Pack deletion (non-active packs)

---

## Training Data Collection

### Design (from ContributeScreen)

Users can opt-in to contribute sign language video clips to improve model accuracy:

**Contribution system:**
- Toggle: "Share my sign samples" (opt-in)
- Daily missions: "Record 5 greetings" (gamified)
- Progress tracking: "38 clips recorded, 2 badges earned"
- Upload progress indicator
- Privacy assurance: "Your data is handled responsibly"

**What needs to be built:**
- Video recording pipeline
- Clip segmentation and labeling
- Secure upload with consent tracking
- Mission/task system
- Contribution analytics
- Privacy/consent framework (GDPR, COPPA compliance)

---

## Implementation Status

| Component | Status | Notes |
|---|---|---|
| Camera integration | Not implemented | Gradient placeholder viewport |
| Hand detection | Not implemented | Static detection brackets |
| Sign recognition | Not implemented | Hardcoded "Thank you", 96% |
| Text display | Implemented (UI) | Renders mock results correctly |
| Text-to-speech | **Implemented** | Platform-native TTS via expect/actual |
| Confidence scoring | Not implemented | Hardcoded values |
| Low-confidence flow | Implemented (UI) | Screen and suggestions render correctly |
| Offline models | Not implemented | Mock storage and pack data |
| Model download | Not implemented | Frozen progress UI |
| Data contribution | Not implemented | Mock missions and stats |
| Practice evaluation | Not implemented | Hardcoded 84% match |

### Recommended Implementation Order

1. **Camera integration** — CameraX/AVCaptureSession via expect/actual
2. **Hand detection** — MediaPipe Hands or custom landmark model
3. **Sign classification** — TFLite/CoreML model with ASL vocabulary
4. **Pipeline integration** — Connect camera → detection → recognition → UI
5. **Confidence scoring** — Real probability outputs from model
6. **Model management** — Download, versioning, storage
7. **Practice evaluation** — Compare user gesture to reference
8. **Data contribution** — Recording, upload, consent
