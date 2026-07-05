# Design System

## Table of Contents

- [Overview](#overview)
- [Color Palette](#color-palette)
- [Typography](#typography)
- [Iconography](#iconography)
- [Spacing](#spacing)
- [Shapes](#shapes)
- [Components](#components)
- [Patterns](#patterns)

---

## Overview

Sayva uses Material Design 3 with a custom theme applied via `SayvaTheme`. The design language emphasizes accessibility, warmth, and clarity — critical for a sign language translation app serving the Deaf and hard-of-hearing community.

**Key design decisions:**
- Light theme only (no dark mode implementation; toggle exists in Settings UI but is non-functional)
- Plus Jakarta Sans as the sole text font (geometric sans-serif, high readability)
- Material Symbols Rounded for all iconography (rendered as font glyphs, not vector drawables)
- Pill-shaped primary actions, rounded cards, generous spacing

---

## Color Palette

### Primary — Electric Indigo

The primary brand color, used for CTAs, active states, and brand accents.

| Token | Hex | Role |
|---|---|---|
| `Primary40` | `#5B5FEF` | Primary buttons, active tabs, links |
| `Primary60` | `#8A8EF5` | Hover/focus states, secondary accents |
| `Primary80` | `#BCBEFF` | Light accents, gradient endpoints |
| `Primary20` | `#1B1E7A` | On-primary-container text |
| `PrimaryContainer` | `#E5E6FF` | Primary container backgrounds |
| `OnPrimaryContainer` | `#1B1E7A` | Text on primary containers |

### Secondary — Warm Coral

Used for warmth, engagement cues, and secondary actions.

| Token | Hex | Role |
|---|---|---|
| `Secondary50` | `#FF6F61` | Secondary buttons, badges, alerts |
| `Secondary70` | `#FF9A8F` | Undo actions, toast accents |
| `SecondaryContainer` | `#FFE2DE` | Secondary container backgrounds |
| `OnSecondaryContainer` | `#8C2F25` | Text on secondary containers |

### Tertiary — Signal Green

Used for success states, progress, and positive feedback.

| Token | Hex | Role |
|---|---|---|
| `Tertiary50` | `#00B894` | Success indicators, progress bars |
| `TertiaryContainer` | `#D4E7DE` | Tertiary container backgrounds |
| `OnTertiaryContainer` | `#005544` | Text on tertiary containers |

### Surface and Neutral

| Token | Hex | Role |
|---|---|---|
| `Surface` | `#FCFCFF` | Page backgrounds |
| `SurfaceDim` | `#F4F4F8` | Slightly recessed surfaces |
| `SurfaceContainer` | `#F0F0F7` | Card backgrounds, input fields |
| `SurfaceContainerHigh` | `#EAEAF2` | Elevated containers |
| `OnSurface` | `#1A1B25` | Primary text, dark toasts |
| `OnSurfaceVariant` | `#5C5E72` | Secondary text, captions |
| `Outline` | `#E2E3ED` | Borders, dividers |
| `OutlineStrong` | `#C7C8D8` | Emphasized borders |

### Semantic

| Token | Hex | Role |
|---|---|---|
| `ErrorColor` | `#DC2C46` | Error states, destructive actions |
| `ErrorContainer` | `#FFF0F2` | Error background |
| `WarningColor` | `#E69500` | Warnings, caution states |
| `WarningContainer` | `#FFF8E8` | Warning background |
| `SuccessColor` | `#00B894` | Same as Tertiary50 |
| `SuccessContainer` | `#D4E7DE` | Same as TertiaryContainer |
| `InfoColor` | `#5B5FEF` | Same as Primary40 |
| `InfoContainer` | `#E5E6FF` | Same as PrimaryContainer |

### Theme Mapping

`SayvaColorScheme` maps these tokens to Material3's `lightColorScheme()`:

```
primary         → Primary40
secondary       → Secondary50
tertiary        → Tertiary50
error           → ErrorColor
background      → Surface
surface         → Surface
surfaceVariant  → SurfaceDim
outline         → Outline
outlineVariant  → OutlineStrong
```

---

## Typography

### Font Family

**Plus Jakarta Sans** — a geometric sans-serif loaded as Compose font resources in 6 weights:

| Weight | Resource | `FontWeight` |
|---|---|---|
| Light | `plus_jakarta_sans_light` | `Light (300)` |
| Regular | `plus_jakarta_sans_regular` | `Normal (400)` |
| Medium | `plus_jakarta_sans_medium` | `Medium (500)` |
| SemiBold | `plus_jakarta_sans_semibold` | `SemiBold (600)` |
| Bold | `plus_jakarta_sans_bold` | `Bold (700)` |
| ExtraBold | `plus_jakarta_sans_extrabold` | `ExtraBold (800)` |

### Type Scale

All 13 Material3 text styles are defined in `sayvaTypography()`:

| Style | Weight | Size | Line Height | Letter Spacing |
|---|---|---|---|---|
| `displayLarge` | Bold | 57sp | 64sp | -0.04sp |
| `headlineLarge` | SemiBold | 32sp | 40sp | -0.02sp |
| `headlineMedium` | SemiBold | 24sp | 32sp | -0.01sp |
| `headlineSmall` | Bold | 22sp | 28sp | -0.01sp |
| `titleLarge` | SemiBold | 20sp | 28sp | — |
| `titleMedium` | Bold | 18sp | 24sp | — |
| `titleSmall` | Bold | 16sp | 22sp | — |
| `bodyLarge` | Normal | 16sp | 24sp | — |
| `bodyMedium` | Normal | 14sp | 20sp | — |
| `bodySmall` | Normal | 12sp | 18sp | — |
| `labelLarge` | SemiBold | 14sp | 20sp | 0.04sp |
| `labelMedium` | SemiBold | 12sp | 16sp | 0.02sp |
| `labelSmall` | Medium | 11sp | 16sp | 0.02sp |

---

## Iconography

### Approach

Icons are rendered using **Material Symbols Rounded** as an icon font (not individual vector assets). Two static font files are bundled:

| Font | File | Purpose |
|---|---|---|
| Outline | `material_symbols_outline` | Default icon style (FILL=0) |
| Filled | `material_symbols_filled` | Active/selected states (FILL=1) |

### Icon Rendering

`SymbolIcon` composable renders glyphs via `BasicText`:

```kotlin
SymbolIcon(
    name = "videocam",    // Icon name from MaterialSymbol map
    size = 24.dp,         // Both width/height and font size
    color = Primary40,    // Tint color
    filled = false,       // false = outline, true = filled
)
```

### Icon Registry

`MaterialSymbol` object in `Icons.kt` maps 85+ icon names to Unicode codepoints. Icons are looked up by name string at runtime via `MaterialSymbol.glyph(name)`.

**Commonly used icons across the app:**

| Icon | Usage |
|---|---|
| `arrow_back` | Top bar back navigation |
| `videocam` | Camera/translate actions |
| `home` / `school` / `person` | Bottom navigation tabs |
| `sign_language` | Sign language features |
| `auto_awesome` | AI-powered features |
| `check_circle` | Success states |
| `warning` | Low confidence, caution |
| `favorite` | Favorites |
| `search` | Search bars |
| `settings` | Settings access |
| `mic` | Microphone/voice input |
| `volume_up` | Text-to-speech playback |

---

## Spacing

Defined in `SayvaSpacing` object:

| Token | Value |
|---|---|
| `xxs` | 4.dp |
| `xs` | 8.dp |
| `sm` | 12.dp |
| `md` | 16.dp |
| `lg` | 24.dp |
| `xl` | 32.dp |
| `xxl` | 48.dp |

**Common usage patterns:**
- Screen horizontal padding: 16.dp (`md`)
- Card internal padding: 14–20.dp
- Spacing between sections: 14–24.dp
- Bottom safe area: 80.dp (for bottom nav clearance)

---

## Shapes

Defined in `SayvaShape` object and mapped to Material3 `Shapes`:

| Token | Corner Radius | Material3 Mapping |
|---|---|---|
| `xs` | 4.dp | `extraSmall` |
| `sm` | 8.dp | `small` |
| `md` | 12.dp | `medium` |
| `lg` | 16.dp | `large` |
| `xl` | 24.dp | `extraLarge` |
| `pill` | 100% (50%) | — |

**Common usage:**
- Cards: 14–24.dp rounded corners
- Buttons: pill shape (100% rounded)
- Chips/pills: 100% rounded
- Input fields: 14–16.dp rounded
- Bottom sheet style: 18.dp top corners

---

## Components

See [UI Components](UI_COMPONENTS.md) for the full shared component catalog.

---

## Patterns

### Gradient Cards

Many feature cards use linear gradients for visual emphasis:

- **Translate Now (Home):** `Primary40` → `Primary60`
- **Success State:** `Tertiary50` → `#6BCFAB`
- **Family Banner:** `Secondary50` → `WarningColor`
- **Progress Streak:** `Primary40` → `Primary60`

### Dark Surfaces

Dark-background elements use `OnSurface` (`#1A1B25`) as background with white text:

- Toast notifications (Saved, Offline)
- Loading state card
- Parental controls bar
- Camera viewport overlay

### State Indicators

Confidence levels use color coding:
- **High (≥90%):** `Tertiary50` (green)
- **Medium (60–89%):** `WarningColor` (amber)
- **Low (<60%):** `ErrorColor` (red)

### Section Labels

Uppercase `labelMedium` or `labelSmall` in `OnSurfaceVariant` color, with top padding of 14–16.dp.
