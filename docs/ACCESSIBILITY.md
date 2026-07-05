# Accessibility

## Table of Contents

- [Overview](#overview)
- [Design Philosophy](#design-philosophy)
- [Implemented Accessibility Features](#implemented-accessibility-features)
- [Accessibility Settings Screen](#accessibility-settings-screen)
- [Visual Accessibility](#visual-accessibility)
- [Motor Accessibility](#motor-accessibility)
- [Notification Accessibility](#notification-accessibility)
- [Screen Reader Support](#screen-reader-support)
- [Known Gaps](#known-gaps)
- [Recommendations](#recommendations)

---

## Overview

Sayva is designed for Deaf and hard-of-hearing users, making accessibility a core product concern rather than an afterthought. The app includes a dedicated Accessibility settings screen with 8 configurable options, visual-first notification design, and a color system that uses both color and iconography to convey state.

**Current status:** The Accessibility screen UI is fully built with all controls, but toggles are non-functional — no accessibility behaviors are actually applied at runtime.

---

## Design Philosophy

Key principles evident in the codebase:

1. **Visual-first communication** — No feature relies solely on audio. Notifications use "flash pulse + haptic buzz, never just audio" (from NotificationsScreen footer).

2. **Color + icon redundancy** — States are communicated through both color and symbol:
   - Success: green color + `check_circle` icon
   - Error: red color + `warning` / `no_photography` icon
   - Low confidence: amber color + `warning` icon

3. **Large touch targets** — Circular interactive elements are consistently 38–48.dp, exceeding the 44pt minimum recommended by WCAG.

4. **High contrast text** — Primary text (`OnSurface` = `#1A1B25`) on light backgrounds (`Surface` = `#FCFCFF`) provides a contrast ratio above 15:1.

---

## Implemented Accessibility Features

| Feature | Implementation | Functional |
|---|---|---|
| Color + icon state indicators | Consistently applied across all screens | Yes |
| Large touch targets (38–48dp) | All interactive circular elements | Yes |
| High contrast text | `OnSurface` on `Surface` > 15:1 ratio | Yes |
| Visual-first notifications | No audio-only alerts | Yes (by design) |
| Text-to-speech output | Platform TTS via expect/actual | Yes |
| Accessibility settings UI | 8 configurable options | UI only |
| Screen reader support | No `contentDescription` or semantics | No |

---

## Accessibility Settings Screen

**File:** `AccessibilityScreen.kt`

Eight configurable options presented as toggle/control rows:

### 1. Easy Mode
- **UI:** Hero card with gradient background and large toggle switch
- **Description:** "Simplify interface, enlarge targets, slow animations"
- **State variable:** `easyMode: Boolean`
- **Functional:** No — toggle updates local state only

### 2. Larger Text
- **UI:** Toggle row
- **Description:** "18 pt minimum, scales all text"
- **State variable:** `largerText: Boolean`
- **Functional:** No — does not modify typography

### 3. High Contrast
- **UI:** Toggle row
- **Description:** "Sharpen edges and boost contrast"
- **State variable:** `highContrast: Boolean`
- **Functional:** No — does not modify color scheme

### 4. Color Blind Mode
- **UI:** Segmented button group (4 options)
- **Options:** Off, Deuteranopia, Protanopia, Tritanopia
- **State variable:** `colorBlindMode: String`
- **Functional:** No — does not apply color filters

### 5. Left-Handed Mode
- **UI:** Toggle row
- **Description:** "Mirror interface for left hand"
- **State variable:** `leftHanded: Boolean`
- **Functional:** No — does not mirror layout

### 6. Haptic Feedback
- **UI:** Slider (0–100%) with vibration icon
- **Description:** Intensity control for haptic feedback
- **Visual:** 5 tick marks on slider track
- **Functional:** No — no haptic engine integration

### 7. Reduce Motion
- **UI:** Toggle row
- **Description:** "Minimize animations"
- **State variable:** `reduceMotion: Boolean`
- **Functional:** No — there are minimal animations in the app currently

### 8. Screen Reader Hints
- **UI:** Toggle row
- **Description:** "TalkBack + BrailleBack support"
- **State variable:** `screenReaderHints: Boolean`
- **Functional:** No — no Compose semantics annotations present

### Roadmap Text
The screen includes a footer: "Coming next: Signing avatars, AR overlays, vibration patterns for alerts"

---

## Visual Accessibility

### Color Contrast

The design system provides strong default contrast ratios:

| Combination | Ratio (approx.) | WCAG Level |
|---|---|---|
| `OnSurface` (#1A1B25) on `Surface` (#FCFCFF) | ~16:1 | AAA |
| `OnSurfaceVariant` (#5C5E72) on `Surface` (#FCFCFF) | ~6:1 | AA |
| White on `Primary40` (#5B5FEF) | ~5:1 | AA |
| White on `ErrorColor` (#DC2C46) | ~4.5:1 | AA |
| White on `Tertiary50` (#00B894) | ~3.5:1 | AA (large text only) |
| White on `Secondary50` (#FF6F61) | ~3.2:1 | AA (large text only) |

### Color Independence

States are not communicated through color alone:

- **Success:** Green background + `check_circle` icon + text label
- **Error:** Red background + error icon + descriptive text + action buttons
- **Low confidence:** Amber/red + warning icon + percentage text
- **Favorites:** Star icon (filled/outline) + color change
- **Active tab:** Filled icon + chip background + text label

### Font Sizing

Typography ranges from 11sp (`labelSmall`) to 57sp (`displayLarge`). The larger text accessibility option would set a minimum of 18sp, but this is not implemented.

---

## Motor Accessibility

### Touch Target Sizes

| Element | Size | Meets 44dp minimum |
|---|---|---|
| Back button circles | 38.dp | Close (not quite) |
| Action tiles | 48.dp+ | Yes |
| Bottom nav tabs | Full-width segments | Yes |
| FAB buttons | 56.dp | Yes |
| Toggle switches | 42 × 24.dp | Track small; needs expansion |
| Filter chips | ~40 × 36.dp | Close |

### Left-Handed Mode

Designed but not implemented. The UI option exists to "mirror interface for left hand" which would flip navigation elements and primary action placement.

---

## Notification Accessibility

From the NotificationsScreen footer text:

> "Sayva alerts are visual-first — flash pulse + haptic buzz, never just audio."

This design decision is critical for Deaf users who cannot rely on audio notifications. The notification system is designed around:

1. **Visual indicators:** Red dot badges, highlighted rows, on-screen alerts
2. **Haptic feedback:** Vibration patterns (designed, not implemented)
3. **Flash alerts:** Screen flash pulse (designed, not implemented)
4. **No audio dependency:** No notification sounds in the design

---

## Screen Reader Support

### Current State

**Not implemented.** No Compose `Modifier.semantics {}` blocks, `contentDescription` values, or accessibility labels are present in the codebase.

### Impact Areas

| Element | Screen Reader Need |
|---|---|
| `SymbolIcon` | Needs `contentDescription` — currently renders as invisible to screen readers |
| Gradient cards | Need semantic grouping and descriptive labels |
| Custom toggles | Need `Role.Switch` semantics |
| Filter chips | Need `Role.Tab` or selection state semantics |
| Progress bars | Need value announcements |
| Chat bubbles | Need message role and content semantics |
| Detection brackets | Decorative, should be hidden from accessibility tree |
| Confidence percentages | Need live region announcements |

---

## Known Gaps

1. **No Compose semantics annotations** — Screen readers cannot interpret custom components
2. **No `contentDescription` on icons** — `SymbolIcon` uses `BasicText` with no accessibility metadata
3. **Accessibility toggles are non-functional** — All 8 options are UI-only
4. **No RTL layout support** — Layout is hardcoded LTR
5. **Toggle switch touch targets** — Custom toggles (42 × 24.dp) are below WCAG minimum
6. **No focus management** — Keyboard/switch navigation not considered
7. **No live regions** — Dynamic content (confidence scores, recognition results) not announced
8. **Color-blind mode not applied** — Segmented control exists but doesn't filter colors
9. **Back button slightly undersized** — 38.dp vs. recommended 44.dp minimum

---

## Recommendations

### Priority 1 — Screen Reader Basics

1. Add `Modifier.semantics { contentDescription = "..." }` to all `SymbolIcon` instances
2. Apply `Role.Button`, `Role.Switch`, `Role.Tab` to interactive custom components
3. Add `liveRegion = LiveRegionMode.Polite` to confidence score and recognition result displays
4. Mark decorative elements (detection brackets, gradient overlays) with `clearAndSetSemantics {}`

### Priority 2 — Functional Toggles

1. Wire Easy Mode to increase minimum touch targets and text sizes
2. Wire High Contrast to swap color scheme
3. Wire Color Blind Mode to apply daltonization filters
4. Wire Reduce Motion to disable any future animations
5. Persist settings via SharedPreferences / UserDefaults

### Priority 3 — Enhanced Support

1. Add BrailleBack/VoiceOver testing and optimization
2. Implement haptic feedback patterns for recognition events
3. Add flash alerts for notifications
4. Support dynamic type / system font scaling
5. Add keyboard/switch control navigation support
6. Implement RTL layout mirroring
