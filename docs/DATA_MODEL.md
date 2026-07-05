# Data Model

## Table of Contents

- [Overview](#overview)
- [Data Classes](#data-classes)
- [Mock Data](#mock-data)
- [Data Flow](#data-flow)
- [Storage](#storage)

---

## Overview

All data in Sayva is modeled via Kotlin data classes in `shared/src/commonMain/kotlin/org/moashraf/sayva/data/Models.kt` and populated by `MockSayvaData.kt`. There is no database, no network layer, and no persistence. Data is read-only at the object level; some screens use local `mutableStateOf` for session-scoped state.

---

## Data Classes

### HistoryItem

A single translation history entry.

```kotlin
data class HistoryItem(
    val id: String,
    val day: String,              // Grouping label: "TODAY", "YESTERDAY"
    val time: String,             // Display time: "2:31 PM"
    val title: String,            // Recognized sign or conversation title
    val confidence: Int? = null,  // 0–100 recognition confidence
    val language: String? = null, // e.g., "ASL"
    val isFavorite: Boolean = false,
    val isConversation: Boolean = false,
    val messageCount: Int? = null, // For conversations
    val duration: String? = null,  // e.g., "4 min"
    val isLowConfidence: Boolean = false,
)
```

**Used by:** HistoryScreen (list), HistoryDetailScreen (lookup by id)

---

### HistoryDetail

Extended detail for a single history entry.

```kotlin
data class HistoryDetail(
    val id: String,
    val recognizedSign: String,   // e.g., "Thank you"
    val confidence: Int,          // 0–100
    val language: String,         // e.g., "ASL"
    val timeLabel: String,        // e.g., "Today · 2:31 PM"
    val spokenAs: String,         // TTS text: "Thank you very much"
)
```

**Used by:** HistoryDetailScreen

---

### FavoritePhrase

A saved quick-access phrase for TTS.

```kotlin
data class FavoritePhrase(
    val id: String,
    val category: String,         // "Greetings", "Medical", etc.
    val text: String,             // Phrase text
    val gradient: List<Color>,    // Card background gradient colors
    val icon: String,             // MaterialSymbol icon name
    val isEmergency: Boolean = false,
)
```

**Used by:** FavoritesScreen

---

### SavedConversation

A saved two-way conversation transcript.

```kotlin
data class SavedConversation(
    val id: String,
    val title: String,
    val timeLabel: String,
    val preview: String,          // First line preview
    val partnerInitial: String,   // Avatar initial letter
    val partnerColor: Color,      // Avatar gradient color
    val messageCount: Int? = null,
    val category: String? = null, // e.g., "Medical"
    val categoryColor: Color = Color(0xFF5C5E72),
    val categoryBg: Color = Color(0xFFF0F0F7),
    val isFavorite: Boolean = false,
    val highlighted: Boolean = false,
)
```

**Used by:** SavedConversationsScreen

---

### LearnCategory

A lesson category grouping.

```kotlin
data class LearnCategory(
    val id: String,
    val name: String,             // "Greetings", "Numbers", etc.
    val learned: Int,             // Signs mastered
    val total: Int,               // Total signs in category
    val icon: String,             // MaterialSymbol icon name
    val iconColor: Color,
    val iconBg: Color,
    val barColor: Color,          // Progress bar fill color
)
```

**Used by:** LearnCategoriesScreen

---

### Lesson

An individual sign language lesson.

```kotlin
data class Lesson(
    val id: String,
    val categoryLabel: String,    // e.g., "GREETINGS"
    val indexLabel: String,       // e.g., "Sign 1 of 12"
    val title: String,            // e.g., "Hello 👋"
    val description: String,
    val icon: String,
    val tags: List<String>,       // e.g., ["One hand", "Forward motion"]
)
```

**Used by:** LessonPlayerScreen

---

### QuizQuestion

A practice quiz prompt.

```kotlin
data class QuizQuestion(
    val id: String,
    val targetPhrase: String,     // Phrase user must sign
)
```

**Used by:** PracticeScreen

---

### Badge

A gamification achievement badge.

```kotlin
data class Badge(
    val id: String,
    val name: String,
    val icon: String,
    val iconColor: Color,
    val bg: Color,
    val locked: Boolean = false,
)
```

**Used by:** ProgressScreen

---

### ProgressStats

Aggregated learning progress statistics.

```kotlin
data class ProgressStats(
    val streakDays: Int,
    val personalBestStreak: Int,
    val totalXp: Int,
    val signsLearned: Int,
    val lessonsCompleted: Int,
    val badgesEarned: Int,
    val weeklyHeights: List<Float>,  // Bar chart heights (7 values)
    val weeklyLabels: List<String>,  // Day labels: ["M","T","W","T","F","S","S"]
)
```

**Used by:** ProgressScreen, HomeScreen (streak reference)

---

### NotificationItem

An in-app notification entry.

```kotlin
data class NotificationItem(
    val id: String,
    val group: String,            // "TODAY", "THIS WEEK"
    val title: String,
    val body: String,
    val icon: String,
    val iconColor: Color,
    val iconBg: Color,
    val timeAgo: String,          // e.g., "2 h ago"
    val highlighted: Boolean = false,
    val actionLabel: String? = null,
)
```

**Used by:** NotificationsScreen

---

### RecognitionSuggestion

An alternative AI recognition when confidence is low.

```kotlin
data class RecognitionSuggestion(
    val sign: String,             // Suggested sign name
    val confidence: Int,          // 0–100
    val description: String,
    val icon: String = "sign_language",
)
```

**Used by:** AiFeedbackLowConfidenceScreen

---

### OfflineLanguagePack

A downloadable language model pack.

```kotlin
data class OfflineLanguagePack(
    val id: String,
    val flag: String,             // Emoji flag: "🇺🇸", "🇬🇧"
    val name: String,             // "ASL", "BSL"
    val detail: String,           // "American Sign Language"
    val sizeMb: Int,
    val isActive: Boolean = false,
    val isBeta: Boolean = false,
    val isDownloaded: Boolean = false,
)
```

**Used by:** OfflineModelsScreen

---

### FamilyMember

A family plan member.

```kotlin
data class FamilyMember(
    val id: String,
    val initial: String,          // Avatar letter
    val gradient: List<Color>,    // Avatar gradient colors
    val name: String,
    val detail: String,           // e.g., "12-day streak · 847 XP"
    val badge: String? = null,    // "ADMIN", "KID"
    val badgeColor: Color = Color(0xFF5C5E72),
    val badgeBg: Color = Color(0xFFF0F0F7),
)
```

**Used by:** FamilyScreen

---

### InterpreterOption

A professional interpreter service option.

```kotlin
data class InterpreterOption(
    val id: String,
    val name: String,             // "Sorenson VRS"
    val detail: String,
    val waitTime: String,         // "~2 min"
    val priceLabel: String,       // "Free with VRS"
    val icon: String = "videocam",
    val featured: Boolean = false,
)
```

**Used by:** InterpreterHandoffScreen

---

## Mock Data

All mock data is defined in `MockSayvaData` object (`data/MockSayvaData.kt`).

### Data Inventory

| Collection | Type | Count | Key Values |
|---|---|---|---|
| `history` | `List<HistoryItem>` | 5 | Thank you (96%), Hello (89%), conversation, I need help (42%), Good morning (94%) |
| `historyDetails` | `Map<String, HistoryDetail>` | 1 | "h1" → Thank you, 96%, ASL |
| `favorites` | `List<FavoritePhrase>` | 4 | Hello, I need help (emergency), Thank you, Where is...? |
| `savedConversations` | `List<SavedConversation>` | 3 | Coffee order, Doctor appointment (highlighted), Study group |
| `learnCategories` | `List<LearnCategory>` | 6 | Greetings (8/12), Numbers (3/10), Family (5/8), Food (0/15), Medical (2/10), Emotions (7/12) |
| `lessons` | `Map<String, Lesson>` | 1 | "l-hello" → Hello lesson with tags |
| `quizQuestions` | `List<QuizQuestion>` | 5 | Hello, Thank you, My name is Alex, Where is the bathroom?, I need help |
| `badges` | `List<Badge>` | 4 | First Sign (unlocked), 7-Day Streak (unlocked), Speed Demon (unlocked), Polyglot (locked) |
| `progressStats` | `ProgressStats` | 1 | 12-day streak, personal best 18, 847 XP, 38 signs, 12 lessons, 3 badges |
| `notifications` | `List<NotificationItem>` | 5 | Model update, streak warning, badge earned, family invite, weekly recap |
| `lowConfidenceSuggestions` | `List<RecognitionSuggestion>` | 3 | Thank you (72%), Please (58%), Sorry (41%) |
| `offlinePacks` | `List<OfflineLanguagePack>` | 4 | ASL (active, 182 MB), BSL (downloaded, 108 MB), LSF (available, 95 MB), JSL (beta, 87 MB) |
| `familyMembers` | `List<FamilyMember>` | 4 | Jordan (owner), Mom (ADMIN), Dad, Ella (KID) |
| `interpreterOptions` | `List<InterpreterOption>` | 3 | Sorenson VRS (featured), Convo Relay, ZP InSight |

---

## Data Flow

```
MockSayvaData (object)
      │
      │  Direct property access
      ▼
Screen Composables
      │
      │  remember { mutableStateOf(...) }
      ▼
Local UI State (session-scoped)
```

**Read path:** Screens access `MockSayvaData` properties directly. No repository pattern, no ViewModel, no state management framework.

**Write path:** Some screens create local `mutableStateOf` variables for:
- Filter selections (HistoryScreen, FavoritesScreen)
- Toggle states (SettingsScreen, AccessibilityScreen, ContributeScreen)
- Answered/index tracking (PracticeScreen)
- Favorite toggling (HistoryDetailScreen)
- Plan selection (PaywallScreen)

All local state is lost on screen exit or app restart.

---

## Storage

**Current state:** No persistence mechanism exists.

| Storage Type | Status |
|---|---|
| Local database | Not implemented |
| SharedPreferences / UserDefaults | Not implemented |
| File storage | Not implemented |
| Network/API | Not implemented |
| Keychain/KeyStore | Not implemented |

**Permissions declared in AndroidManifest.xml:** None (no camera, mic, storage, or internet permissions).

**iOS Info.plist permissions:** None declared.
