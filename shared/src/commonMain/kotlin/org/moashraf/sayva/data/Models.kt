package org.moashraf.sayva.data

import androidx.compose.ui.graphics.Color

data class HistoryItem(
    val id: String,
    val day: String,
    val time: String,
    val title: String,
    val confidence: Int? = null,
    val language: String? = null,
    val isFavorite: Boolean = false,
    val isConversation: Boolean = false,
    val messageCount: Int? = null,
    val duration: String? = null,
    val isLowConfidence: Boolean = false,
)

data class HistoryDetail(
    val id: String,
    val recognizedSign: String,
    val confidence: Int,
    val language: String,
    val timeLabel: String,
    val spokenAs: String,
)

data class FavoritePhrase(
    val id: String,
    val category: String,
    val text: String,
    val gradient: List<Color>,
    val icon: String,
    val isEmergency: Boolean = false,
)

data class SavedConversation(
    val id: String,
    val title: String,
    val timeLabel: String,
    val preview: String,
    val partnerInitial: String,
    val partnerColor: Color,
    val messageCount: Int? = null,
    val category: String? = null,
    val categoryColor: Color = Color(0xFF5C5E72),
    val categoryBg: Color = Color(0xFFF0F0F7),
    val isFavorite: Boolean = false,
    val highlighted: Boolean = false,
)

data class LearnCategory(
    val id: String,
    val name: String,
    val learned: Int,
    val total: Int,
    val icon: String,
    val iconColor: Color,
    val iconBg: Color,
    val barColor: Color,
)

data class Lesson(
    val id: String,
    val categoryLabel: String,
    val indexLabel: String,
    val title: String,
    val description: String,
    val icon: String,
    val tags: List<String>,
)

data class QuizQuestion(
    val id: String,
    val targetPhrase: String,
)

data class Badge(
    val id: String,
    val name: String,
    val icon: String,
    val iconColor: Color,
    val bg: Color,
    val locked: Boolean = false,
)

data class ProgressStats(
    val streakDays: Int,
    val personalBestStreak: Int,
    val totalXp: Int,
    val signsLearned: Int,
    val lessonsCompleted: Int,
    val badgesEarned: Int,
    val weeklyHeights: List<Float>,
    val weeklyLabels: List<String>,
)

data class NotificationItem(
    val id: String,
    val group: String,
    val title: String,
    val body: String,
    val icon: String,
    val iconColor: Color,
    val iconBg: Color,
    val timeAgo: String,
    val highlighted: Boolean = false,
    val actionLabel: String? = null,
)

data class RecognitionSuggestion(
    val sign: String,
    val confidence: Int,
    val description: String,
    val icon: String = "sign_language",
)

data class OfflineLanguagePack(
    val id: String,
    val flag: String,
    val name: String,
    val detail: String,
    val sizeMb: Int,
    val isActive: Boolean = false,
    val isBeta: Boolean = false,
    val isDownloaded: Boolean = false,
)

data class FamilyMember(
    val id: String,
    val initial: String,
    val gradient: List<Color>,
    val name: String,
    val detail: String,
    val badge: String? = null,
    val badgeColor: Color = Color(0xFF5C5E72),
    val badgeBg: Color = Color(0xFFF0F0F7),
)

data class InterpreterOption(
    val id: String,
    val name: String,
    val detail: String,
    val waitTime: String,
    val priceLabel: String,
    val icon: String = "videocam",
    val featured: Boolean = false,
)
