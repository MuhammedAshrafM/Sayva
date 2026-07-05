# UI Components

## Table of Contents

- [Shared Components](#shared-components)
  - [SayvaTopBar](#sayvatopbar)
  - [SayvaBottomNav](#sayvabottomnav)
  - [PrimaryButton](#primarybutton)
  - [SecondaryButton](#secondarybutton)
  - [TextLink](#textlink)
  - [Pill](#pill)
  - [SymbolIcon](#symbolicon)
- [Screen-Local Components](#screen-local-components)
  - [Cards](#cards)
  - [Input Fields](#input-fields)
  - [Toggles and Switches](#toggles-and-switches)
  - [Filter Chips](#filter-chips)
  - [List Rows](#list-rows)
  - [State Cards](#state-cards)
  - [Chat Bubbles](#chat-bubbles)
  - [Progress Indicators](#progress-indicators)
  - [Toasts](#toasts)
- [Layout Patterns](#layout-patterns)

---

## Shared Components

These composables are defined in `shared/src/commonMain/kotlin/org/moashraf/sayva/ui/components/` and reused across screens.

---

### SayvaTopBar

**File:** `Bars.kt`

Standard top navigation bar with back button, title, and optional trailing content.

| Parameter | Type | Description |
|---|---|---|
| `onBack` | `() -> Unit` | Back button click handler |
| `title` | `String` | Center-aligned title text |
| `trailing` | `@Composable (() -> Unit)?` | Optional trailing slot (icons, buttons) |

**Appearance:**
- Height: standard row with 16.dp horizontal, 8.dp vertical padding
- Back button: 38.dp circle with `SurfaceContainer` background, `arrow_back` icon
- Title: `titleMedium` typography
- Trailing slot: right-aligned

**Used by:** Permissions, Login, Register, ForgotPassword, Progress, Family, Settings, Accessibility, PairSecondScreen, and others.

---

### SayvaBottomNav

**File:** `Bars.kt`

Four-tab bottom navigation bar shown only on root screens.

| Parameter | Type | Description |
|---|---|---|
| `currentTab` | `BottomTab` | Currently selected tab |
| `onTabSelected` | `(BottomTab) -> Unit` | Tab selection callback |

**Tabs:**

| Tab | Icon (outline) | Icon (filled) | Label |
|---|---|---|---|
| Home | `home` | `home` (filled) | Home |
| Translate | `videocam` | `videocam` (filled) | Translate |
| Learn | `school` | `school` (filled) | Learn |
| You | `person` | `person` (filled) | You |

**Appearance:**
- Background: `Surface` with `Outline` top border
- Selected tab: rounded pill chip background, filled icon, colored text
- Unselected tab: outline icon, `OnSurfaceVariant` text
- Label: `labelSmall` typography

**Visibility:** Only shown when `current` screen is one of: `Home`, `LiveCamera`, `LearnCategories`, `Profile`.

---

### PrimaryButton

**File:** `Buttons.kt`

Full-width solid pill button for primary actions.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String` | — | Button label |
| `onClick` | `() -> Unit` | — | Click handler |
| `enabled` | `Boolean` | `true` | Enabled state |
| `leadingIcon` | `String?` | `null` | Optional leading icon name |
| `trailingIcon` | `String?` | `null` | Optional trailing icon name |
| `backgroundColor` | `Color` | `Primary40` | Background color |
| `contentColor` | `Color` | `Color.White` | Text and icon color |

**Appearance:**
- Shape: pill (100% rounded corners)
- Width: `fillMaxWidth()`
- Padding: 14.dp vertical
- Typography: `labelLarge`
- Disabled: background at 0.4 alpha

---

### SecondaryButton

**File:** `Buttons.kt`

Full-width outlined pill button for secondary actions.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String` | — | Button label |
| `onClick` | `() -> Unit` | — | Click handler |
| `leadingIcon` | `String?` | `null` | Optional leading icon name |

**Appearance:**
- Shape: pill (100% rounded corners)
- Width: `fillMaxWidth()`
- Background: `Color.White`
- Border: 1.5.dp, `OutlineStrong`
- Text color: `OnSurface`
- Icon color: `Primary40`
- Padding: 14.dp vertical
- Typography: `labelLarge`

---

### TextLink

**File:** `Buttons.kt`

Plain clickable text link for tertiary actions.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String` | — | Link text |
| `onClick` | `() -> Unit` | — | Click handler |
| `color` | `Color` | `Primary40` | Text color |

**Appearance:**
- Typography: `labelLarge`
- No background, border, or padding (inline)

---

### Pill

**File:** `Misc.kt`

Small rounded chip for labels, tags, and badges.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `text` | `String` | — | Chip text |
| `backgroundColor` | `Color` | — | Background color |
| `contentColor` | `Color` | — | Text color |
| `icon` | `String?` | `null` | Optional leading icon name |

**Appearance:**
- Shape: pill (100% rounded)
- Padding: 8.dp horizontal, 4.dp vertical
- Typography: `labelSmall`
- Icon size: 12.dp (if present)

---

### SymbolIcon

**File:** `designsystem/SymbolIcon.kt`

Material Symbols icon rendered as a font glyph.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `name` | `String` | — | Icon name from `MaterialSymbol` registry |
| `size` | `Dp` | `24.dp` | Icon dimensions and font size |
| `color` | `Color` | `Color.Unspecified` | Tint color |
| `filled` | `Boolean` | `false` | Use filled (true) or outline (false) variant |
| `modifier` | `Modifier` | `Modifier` | Additional modifiers |

**Implementation:** Renders via `BasicText` with icon font family. Uses `MaterialSymbol.glyph(name)` to resolve Unicode codepoint.

---

## Screen-Local Components

These patterns are implemented inline within individual screens, not as shared composables.

---

### Cards

**Gradient Hero Card** — Used for prominent feature callouts.
- LinearGradient background with rounded corners (18–24.dp)
- Large semi-transparent icon overlay (100–120.dp, 0.18 alpha)
- Title + subtitle in white text
- Used in: Home (Translate Now), LearnCategories (Daily Challenge), Family (Banner), Contribute (Impact), Progress (Streak)

**Surface Card** — Standard content card.
- `SurfaceContainer` background with 14–18.dp rounded corners
- Internal padding 12–20.dp
- Optional border using `Outline` or `OutlineStrong`
- Used in: History rows, Settings sections, Notification rows

**Dark Card** — Inverted color card for contrast.
- `OnSurface` background with white text
- Used in: Loading state, Toasts, Parental controls, Camera viewport

---

### Input Fields

**LabeledField** — Text input with label header and focus indicator.
- Implemented inline in LoginScreen, RegisterScreen, ForgotPasswordScreen
- Label: `labelSmall` in `OnSurfaceVariant`
- Field: `SurfaceContainer` background, 14.dp rounded corners
- Focus state: `Primary40` border (1.5.dp)
- Suffix icon slot (e.g., visibility toggle for password)
- Not a shared composable — reimplemented per screen

---

### Toggles and Switches

**Custom Toggle** — Pill-shaped toggle switch.
- Implemented inline across Settings, Accessibility, Contribute, Family screens
- Track: 42.dp × 24.dp rounded pill
- Thumb: 20.dp circle, aligned to start (off) or end (on)
- ON color: `Secondary50` or `Primary40`
- OFF color: `OutlineStrong`
- Not using Material3 `Switch` composable — custom drawn

---

### Filter Chips

**Horizontal Filter Row** — `LazyRow` of selectable chips.
- Each chip: `SurfaceContainer` background (unselected) or `OnSurface`/`Primary40` background (selected)
- Text: `labelMedium`
- Padding: 10.dp horizontal, 8.dp vertical
- Shape: pill (100% rounded)
- Used in: History, Favorites, HistoryDetail

---

### List Rows

**Menu Row** — Profile-style navigation row.
- Leading: colored icon in rounded-corner box (38–44.dp)
- Title: `titleSmall`
- Subtitle: `labelSmall` in `OnSurfaceVariant`
- Trailing: chevron_right icon or badge
- Full-width with 14.dp horizontal padding
- Used in: Profile (9 rows), Settings (multiple sections)

**Notification Row** — Notification list item.
- Optional red dot indicator (8.dp circle)
- Leading: icon with colored background circle
- Title + body + time metadata
- Optional action label
- Highlighted rows have tinted icon background

**Family Member Row** — Member list item.
- Gradient avatar circle with initial letter
- Name + detail text
- Trailing: badge text or more_vert icon

---

### State Cards

Four reference state patterns defined in SystemStatesScreen:

**Empty State**
- Centered layout with gradient icon box (88.dp)
- Title: "Nothing here yet"
- Description text
- CTA button

**Loading State**
- Dark (`OnSurface`) background
- Concentric circle animation placeholder
- `auto_awesome` icon
- Title + progress text
- Gradient progress bar

**Error State**
- `ErrorContainer` background with error border
- Error icon in `ErrorColor` box
- Title + description in dark red
- Two action buttons: "Try later" (outlined) + "Open settings" (solid error)

**Success State**
- Green gradient background (`Tertiary50` → `#6BCFAB`)
- Celebration icon overlay (semi-transparent)
- Checkmark in white circle
- Title + XP info
- "Next lesson" button

---

### Chat Bubbles

**TranscriptBubble** — Conversation message bubble.
- Sign bubbles: left-aligned, `PrimaryContainer` background, `Primary40` text
- Voice bubbles: right-aligned, `SurfaceContainer` background, `OnSurface` text
- Metadata line: "Signed · 2:31 PM" / "Voice · 2:32 PM"
- Rounded corners: 18.dp with flat corner on alignment side
- Clickable for TTS playback

**TypingBubble** — Animated typing indicator.
- 3 dot circles in sequence
- Pulsing animation (implied, dots are static in code)

---

### Progress Indicators

**Horizontal Progress Bar** — Gradient-filled bar.
- Track: faded color or surface container
- Fill: `linearGradient(Primary40, Primary60)` or single color
- Height: 4–8.dp
- Corner radius: matching height
- Used in: LessonPlayer, Practice, FirstLaunchModelDownload, LearnCategories, Contribute

**Confidence Bar** — Percentage-filled bar with label.
- Shows numeric percentage alongside filled bar
- Color varies by confidence level (green/amber/red)
- Used in: LiveCamera, HistoryDetail, AiFeedbackLowConfidence

**Step Tracker** — Vertical step list with status icons.
- Completed: check circle in `Tertiary50`
- Active: filled circle in `Primary40` with pulsing ring
- Pending: unfilled circle in `OnSurfaceVariant`
- Used in: FirstLaunchModelDownload

**Weekly Bar Chart** — 7-day activity chart.
- Vertical bars with proportional heights
- Today: `WarningColor`, peak: `Primary40`, others: faded
- Day labels below (M T W T F S S)
- Used in: ProgressScreen

---

### Toasts

**Success Toast** — Confirmation notification.
- `OnSurface` background, `bodyMedium` white text
- Leading: `check_circle` icon in `SuccessColor`
- Trailing: "UNDO" text in `Secondary70`
- 12.dp rounded corners

**Offline Toast** — Connectivity notification.
- `OnSurface` background, `bodyMedium` white text
- Leading: `cloud_off` icon in `Secondary70`
- No trailing action

---

## Layout Patterns

### Screen Structure

Most screens follow one of two patterns:

**Scrollable content:**
```
Column(fillMaxSize)
├── SayvaTopBar (or custom top bar)
└── LazyColumn(weight 1f)
    ├── Content sections
    └── Bottom spacer (80.dp for nav clearance)
```

**Fixed layout with scroll body:**
```
Column(fillMaxSize)
├── Top bar
├── Scrollable content (weight 1f)
└── Sticky bottom bar / CTA
```

### Common Spacing
- Screen horizontal padding: 16.dp
- Section spacing: 14–24.dp
- Card internal padding: 12–20.dp
- Icon-to-text spacing: 6–12.dp
- Bottom nav clearance: 80.dp spacer

### Grid Layouts
- Quick actions (Home): `LazyVerticalGrid`, 2 columns
- Categories (Learn): `LazyVerticalGrid`, 2 columns
- Favorites: `LazyVerticalGrid`, 2 columns
- Stats (Profile/Progress): `Row` with equal-weight items
