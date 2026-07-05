package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.moashraf.sayva.data.LearnCategory
import org.moashraf.sayva.data.Lesson
import org.moashraf.sayva.data.ProgressStats
import org.moashraf.sayva.data.QuizQuestion
import org.moashraf.sayva.data.repository.LessonsRepository
import org.moashraf.sayva.data.repository.ProgressRepository
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway

/**
 * Feeds the four Learn screens: `LearnCategoriesScreen`, `LessonPlayerScreen`,
 * `PracticeScreen`, `ProgressScreen`.
 *
 * ### Static vs observed data
 * Learn categories, lessons, and quiz questions are static reference content —
 * we expose them via sync accessors ([lesson], [quiz]). The per-user progress
 * (streaks, XP, learned counts) is live via [progressStats] and drives the
 * `learned` field of [categories] so the category grid reflects the user's
 * real completion count, not the static seed value.
 *
 * ### Completing a practice session
 * [completePracticeSession] records a study session (streak logic), adds XP,
 * increments signs-learned, and increments lessons-completed. The Progress
 * screen and category grid update reactively via [progressStats] +
 * [categories].
 */
class LearnViewModel(
    private val lessonsRepository: LessonsRepository,
    private val progressRepository: ProgressRepository,
    private val analytics: AnalyticsGateway,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** All progress stats — feeds ProgressScreen + the streak chip on Categories. */
    val progressStats: StateFlow<ProgressStats> =
        progressRepository.observe()
            .stateIn(scope, SharingStarted.Eagerly, EMPTY_STATS)

    /**
     * Static category list with the live `learned` count merged in from
     * `LessonsRepository.observeCategoryProgress`. Falls back to the seed
     * `learned/total` on the LearnCategory itself when progress is missing.
     */
    val categories: StateFlow<List<LearnCategory>> =
        lessonsRepository.observeCategoryProgress()
            .combine(kotlinx.coroutines.flow.flowOf(lessonsRepository.getCategories())) { progress, all ->
                all.map { category ->
                    val overlay = progress[category.id]
                    if (overlay == null) category
                    else category.copy(learned = overlay.learned, total = overlay.total)
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, lessonsRepository.getCategories())

    // ---- Sync accessors for static content ----------------------------------

    /** Load one lesson by id. Returns null if the id is unknown. */
    fun lesson(lessonId: String): Lesson? = lessonsRepository.getLesson(lessonId)

    /** Quiz questions for a lesson's practice mode. */
    fun quiz(lessonId: String): List<QuizQuestion> = lessonsRepository.getQuizForLesson(lessonId)

    // ---- Actions ------------------------------------------------------------

    /**
     * Called when the user reaches the end of a PracticeScreen quiz. Awards XP,
     * bumps counters, and records a session (streak). Fire-and-forget — the
     * screen navigates to ProgressScreen without waiting for the DB write.
     *
     * @param xpEarned        total XP for this session (typically the sum of
     *                        per-question XP displayed in the top-right badge).
     * @param signsLearned    how many previously-unknown signs the user got right.
     *                        Callers can pass `questions.size` as a conservative
     *                        upper bound if they don't track per-question outcomes.
     * @param lessonCompleted whether this session completed a whole lesson (vs.
     *                        drilling a single sign). Increments the lessons
     *                        counter only when true.
     */
    fun completePracticeSession(
        xpEarned: Int,
        signsLearned: Int,
        lessonCompleted: Boolean = true,
        lessonId: String? = null,
    ) {
        analytics.logEvent(
            AnalyticsEvents.LEARN_PRACTICE_SESSION_COMPLETED,
            buildMap<String, Any> {
                put(AnalyticsEvents.Param.XP_EARNED, xpEarned)
                put(AnalyticsEvents.Param.SIGNS_LEARNED, signsLearned)
                lessonId?.let { put(AnalyticsEvents.Param.LESSON_ID, it) }
            },
        )
        scope.launch {
            if (xpEarned > 0) progressRepository.addXp(xpEarned)
            repeat(signsLearned.coerceAtLeast(0)) {
                progressRepository.incrementSignsLearned()
            }
            if (lessonCompleted) progressRepository.incrementLessonsCompleted()
            progressRepository.recordSession()
        }
    }

    companion object {
        private val EMPTY_STATS = ProgressStats(
            streakDays = 0,
            personalBestStreak = 0,
            totalXp = 0,
            signsLearned = 0,
            lessonsCompleted = 0,
            badgesEarned = 0,
            weeklyHeights = List(7) { 0f },
            weeklyLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
        )
    }
}
