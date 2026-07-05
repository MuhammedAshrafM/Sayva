package org.moashraf.sayva.data

import androidx.compose.ui.graphics.Color
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.ErrorContainer
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.designsystem.OnWarningContainer
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.designsystem.TertiaryContainer
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.designsystem.WarningContainer

object MockSayvaData {

    val history = listOf(
        HistoryItem(
            id = "h1",
            day = "TODAY",
            time = "9:42 AM",
            title = "\"Thank you very much\"",
            confidence = 96,
            language = "ASL",
            isFavorite = true,
        ),
        HistoryItem(
            id = "h2",
            day = "TODAY",
            time = "9:38 AM",
            title = "\"Where is the bathroom?\"",
            confidence = 88,
            language = "ASL",
        ),
        HistoryItem(
            id = "h3",
            day = "TODAY",
            time = "8:55 AM",
            title = "Doctor visit · 8 messages",
            isConversation = true,
            messageCount = 8,
            duration = "4 min",
        ),
        HistoryItem(
            id = "h4",
            day = "YESTERDAY",
            time = "6:21 PM",
            title = "\"How much does this cost?\"",
            confidence = 91,
            isFavorite = true,
        ),
        HistoryItem(
            id = "h5",
            day = "YESTERDAY",
            time = "2:08 PM",
            title = "Unclear · \"?\" sign",
            confidence = 38,
            isLowConfidence = true,
        ),
    )

    val historyDetails = mapOf(
        "h1" to HistoryDetail(
            id = "h1",
            recognizedSign = "Thank you very much",
            confidence = 96,
            language = "ASL",
            timeLabel = "Today · 9:42 AM",
            spokenAs = "\"Thank you very much.\"",
        ),
    )

    val favorites = listOf(
        FavoritePhrase(
            id = "f1",
            category = "GREETING",
            text = "Hello, nice to meet you",
            gradient = listOf(Primary40, Color(0xFF8A8EF5)),
            icon = "favorite",
        ),
        FavoritePhrase(
            id = "f2",
            category = "MEDICAL",
            text = "I have a headache",
            gradient = listOf(Secondary50, Color(0xFFFF9A8F)),
            icon = "medical_services",
        ),
        FavoritePhrase(
            id = "f3",
            category = "EVERYDAY",
            text = "Could I have water, please?",
            gradient = listOf(Tertiary50, Color(0xFF6BCFAB)),
            icon = "restaurant",
        ),
        FavoritePhrase(
            id = "f4",
            category = "EMERGENCY",
            text = "Call 911, please.",
            gradient = listOf(Color(0xFF1A1B25), Color(0xFF3A3550)),
            icon = "emergency",
            isEmergency = true,
        ),
    )

    val savedConversations = listOf(
        SavedConversation(
            id = "c1",
            title = "Doctor Lee · ENT visit",
            timeLabel = "Today · 8:55 AM · 4 min",
            preview = "\"…my ear hurts since Monday. Which side and is there fluid?…\"",
            partnerInitial = "L",
            partnerColor = Tertiary50,
            messageCount = 12,
            category = "Medical",
            categoryColor = Color(0xFF8C2F25),
            categoryBg = SecondaryContainer,
            isFavorite = true,
            highlighted = true,
        ),
        SavedConversation(
            id = "c2",
            title = "Mom · grocery list",
            timeLabel = "Yesterday · 5 min",
            preview = "\"…milk, eggs, bread, and the pretzel ones…\"",
            partnerInitial = "M",
            partnerColor = Secondary50,
            messageCount = 8,
            category = "Family",
            categoryColor = OnTertiaryContainer,
            categoryBg = TertiaryContainer,
        ),
        SavedConversation(
            id = "c3",
            title = "Barista · coffee order",
            timeLabel = "Mon · 1 min",
            preview = "\"…oat milk latte, extra hot…\"",
            partnerInitial = "B",
            partnerColor = WarningColor,
        ),
    )

    val learnCategories = listOf(
        LearnCategory(
            "greetings",
            "Greetings",
            18,
            24,
            "waving_hand",
            Secondary50,
            SecondaryContainer,
            Secondary50
        ),
        LearnCategory("numbers", "Numbers", 20, 20, "pin", Primary40, PrimaryContainer, Tertiary50),
        LearnCategory(
            "family",
            "Family",
            6,
            30,
            "family_restroom",
            Tertiary50,
            TertiaryContainer,
            Tertiary50
        ),
        LearnCategory(
            "food",
            "Food",
            0,
            40,
            "restaurant",
            WarningColor,
            WarningContainer,
            WarningColor
        ),
        LearnCategory(
            "medical",
            "Medical",
            4,
            50,
            "medical_services",
            ErrorColor,
            ErrorContainer,
            ErrorColor
        ),
        LearnCategory(
            "emotions",
            "Emotions",
            2,
            18,
            "sentiment_satisfied",
            Color(0xFF5C5E72),
            SurfaceContainer,
            Color(0xFF5C5E72)
        ),
    )

    val lessons = listOf(
        Lesson(
            id = "l-hello",
            categoryLabel = "GREETINGS · 12/24",
            indexLabel = "Sign of the day",
            title = "Hello",
            description = "Right hand at forehead, palm out, move forward — like a small salute that travels.",
            icon = "waving_hand",
            tags = listOf("One hand", "Forward motion"),
        ),
    )

    val quizQuestions = listOf(
        QuizQuestion("q1", "Thank you"),
        QuizQuestion("q2", "Thank you"),
        QuizQuestion("q3", "Hello"),
        QuizQuestion("q4", "Please"),
        QuizQuestion("q5", "Goodbye"),
    )

    val badges = listOf(
        Badge("b1", "Week warrior", "whatshot", WarningColor, WarningContainer),
        Badge("b2", "First lesson", "school", Primary40, PrimaryContainer),
        Badge("b3", "Numbers ✓", "workspace_premium", Tertiary50, TertiaryContainer),
        Badge("b4", "30 days", "lock", Color(0xFF5C5E72), SurfaceContainer, locked = true),
    )

    val progressStats = ProgressStats(
        streakDays = 12,
        personalBestStreak = 16,
        totalXp = 847,
        signsLearned = 38,
        lessonsCompleted = 7,
        badgesEarned = 5,
        weeklyHeights = listOf(0.625f, 0.875f, 0.4375f, 0.775f, 1f, 0.5625f, 0.375f),
        weeklyLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
    )

    val notifications = listOf(
        NotificationItem(
            id = "n1",
            group = "TODAY",
            title = "New AI model available",
            body = "v2026.06 is 18% more accurate on two-handed signs. 142 MB",
            icon = "model_training",
            iconColor = Color.White,
            iconBg = Primary40,
            timeAgo = "5 min ago",
            highlighted = true,
            actionLabel = "UPDATE NOW",
        ),
        NotificationItem(
            id = "n2",
            group = "TODAY",
            title = "Keep your 12-day streak alive 🔥",
            body = "60 seconds gets you to 13. You've got this.",
            icon = "whatshot",
            iconColor = WarningColor,
            iconBg = WarningContainer,
            timeAgo = "2 h ago",
        ),
        NotificationItem(
            id = "n3",
            group = "TODAY",
            title = "Badge earned · Week warrior",
            body = "7 days in a row. Tap to see your collection.",
            icon = "workspace_premium",
            iconColor = Tertiary50,
            iconBg = TertiaryContainer,
            timeAgo = "Yesterday · 9:00 PM",
        ),
        NotificationItem(
            id = "n4",
            group = "EARLIER",
            title = "Mom liked your translation",
            body = "\"I love you, mom\" · 96% confidence",
            icon = "favorite",
            iconColor = Secondary50,
            iconBg = SecondaryContainer,
            timeAgo = "3 days ago",
        ),
        NotificationItem(
            id = "n5",
            group = "EARLIER",
            title = "New lesson · Emotions",
            body = "18 new signs available in your library.",
            icon = "school",
            iconColor = Color(0xFF5C5E72),
            iconBg = SurfaceContainer,
            timeAgo = "Last week",
        ),
    )

    val lowConfidenceSuggestions = listOf(
        RecognitionSuggestion("Thanks", 68, "Most likely · similar handshape"),
        RecognitionSuggestion("Hello", 42, "Similar forward motion"),
        RecognitionSuggestion("Goodbye", 28, "Hand near face match"),
    )

    val offlinePacks = listOf(
        OfflineLanguagePack(
            "asl",
            "🇺🇸",
            "ASL · American",
            "v2026.06 · default",
            182,
            isActive = true,
            isDownloaded = true
        ),
        OfflineLanguagePack("bsl", "🇬🇧", "BSL · British", "v2026.05", 108, isDownloaded = true),
        OfflineLanguagePack("lsf", "🇫🇷", "LSF · French", "v2026.04", 124),
        OfflineLanguagePack("jsl", "🇯🇵", "JSL · Japanese", "Beta", 156, isBeta = true),
    )

    val familyMembers = listOf(
        FamilyMember(
            id = "m1",
            initial = "J",
            gradient = listOf(Primary40, Secondary50),
            name = "Jordan · You",
            detail = "Owner · ASL · 🔥 12d streak",
            badge = "ADMIN",
            badgeColor = OnWarningContainer,
            badgeBg = WarningContainer,
        ),
        FamilyMember(
            id = "m2",
            initial = "M",
            gradient = listOf(Tertiary50, Primary40),
            name = "Mom · Linda",
            detail = "Hearing · learning ASL · 🔥 9d",
        ),
        FamilyMember(
            id = "m3",
            initial = "D",
            gradient = listOf(WarningColor, Secondary50),
            name = "Dad · Robert",
            detail = "Hearing · learning ASL · 🔥 3d",
        ),
        FamilyMember(
            id = "m4",
            initial = "E",
            gradient = listOf(Color(0xFF8A8EF5), Color(0xFFBCBEFF)),
            name = "Ella · 8 years",
            detail = "Kid mode · safe content only",
            badge = "KID",
            badgeColor = OnTertiaryContainer,
            badgeBg = TertiaryContainer,
        ),
    )

    val interpreterOptions = listOf(
        InterpreterOption(
            id = "i1",
            name = "Sorenson VRS",
            detail = "ASL · medical specialty available",
            waitTime = "~12 s wait",
            priceLabel = "Free (FCC funded)",
            featured = true,
        ),
        InterpreterOption(
            id = "i2",
            name = "Convo Relay",
            detail = "ASL · ~28 s wait · Free",
            waitTime = "~28 s wait",
            priceLabel = "Free",
        ),
        InterpreterOption(
            id = "i3",
            name = "ZP InSight (BSL)",
            detail = "~1 m wait · NHS partner",
            waitTime = "~1 m wait",
            priceLabel = "NHS partner",
        ),
    )
}
