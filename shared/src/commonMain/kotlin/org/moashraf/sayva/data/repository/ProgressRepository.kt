package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.Flow
import org.moashraf.sayva.data.ProgressStats

/**
 * Consumer of `ProgressScreen`, `LearnCategoriesScreen`'s streak badge, and
 * `HomeScreen`'s daily challenge card.
 *
 * Persistence: backed by SQLDelight `ProgressStatsEntity` (single-row table).
 * Increment operations are atomic at the SQL level, safe from concurrent writes.
 */
interface ProgressRepository {

    /** Current progress state. Emits again on any update. */
    fun observe(): Flow<ProgressStats>

    /** Snapshot without subscribing. */
    suspend fun get(): ProgressStats

    /** Add XP earned. Non-negative. */
    suspend fun addXp(amount: Int)

    /** Increment the "signs learned" counter by 1. */
    suspend fun incrementSignsLearned()

    /** Increment the "lessons completed" counter by 1. */
    suspend fun incrementLessonsCompleted()

    /**
     * Record that a study session happened today. Handles streak logic —
     * increments streakDays if the last session was yesterday, resets to 1 if
     * gap > 1 day, no-op if last session was today.
     */
    suspend fun recordSession()

    /** Reset all stats. Used by settings' "reset to defaults" action. */
    suspend fun reset()
}
