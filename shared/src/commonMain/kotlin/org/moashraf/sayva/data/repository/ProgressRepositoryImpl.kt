package org.moashraf.sayva.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.moashraf.sayva.data.ProgressStats
import org.moashraf.sayva.db.ProgressStatsEntity
import org.moashraf.sayva.db.SayvaDatabase

/**
 * SQLDelight-backed [ProgressRepository]. Single-row table via `CHECK (id = 1)`.
 *
 * ### Streak logic
 * Handled in [recordSession] using local-timezone day boundaries — a session at
 * 11:59 PM followed by 12:01 AM should count as two days for streak purposes,
 * even though they're ~2 minutes apart. Timezone: device-local at the time of
 * writing, deliberate — matches user expectation.
 *
 * ### Empty-state
 * If the row doesn't exist yet, [observe] and [get] return a default
 * [ProgressStats]. Writers always upsert, so the row appears on first write.
 */
class ProgressRepositoryImpl(
    database: SayvaDatabase,
) : ProgressRepository {

    private val queries = database.progressStatsQueries

    override fun observe(): Flow<ProgressStats> =
        queries.selectStats().asFlow().mapToOneOrNull(Dispatchers.Default).map {
            it?.toDomain() ?: default()
        }

    override suspend fun get(): ProgressStats =
        queries.selectStats().executeAsOneOrNull()?.toDomain() ?: default()

    override suspend fun addXp(amount: Int) {
        require(amount >= 0) { "XP amount must be non-negative, got $amount" }
        ensureRowExists()
        queries.incrementXp(amount.toLong())
    }

    override suspend fun incrementSignsLearned() {
        ensureRowExists()
        queries.incrementSignsLearned()
    }

    override suspend fun incrementLessonsCompleted() {
        ensureRowExists()
        queries.incrementLessonsCompleted()
    }

    override suspend fun recordSession() {
        val now = Clock.System.now()
        val current = queries.selectStats().executeAsOneOrNull()?.toDomain() ?: default()
        val newStreak = calculateNewStreak(
            previousStreak = current.streakDays,
            lastSessionAt = queries.selectStats().executeAsOneOrNull()?.lastSessionAt,
            nowMillis = now.toEpochMilliseconds(),
        )
        val newPersonalBest = maxOf(current.personalBestStreak, newStreak)
        queries.upsert(
            streakDays = newStreak.toLong(),
            personalBestStreak = newPersonalBest.toLong(),
            totalXp = current.totalXp.toLong(),
            signsLearned = current.signsLearned.toLong(),
            lessonsCompleted = current.lessonsCompleted.toLong(),
            badgesEarned = current.badgesEarned.toLong(),
            lastSessionAt = now.toEpochMilliseconds(),
        )
    }

    override suspend fun reset() {
        queries.reset()
    }

    // -----------------------------------------------------------------

    private fun ensureRowExists() {
        if (queries.selectStats().executeAsOneOrNull() != null) return
        val fresh = default()
        queries.upsert(
            streakDays = fresh.streakDays.toLong(),
            personalBestStreak = fresh.personalBestStreak.toLong(),
            totalXp = fresh.totalXp.toLong(),
            signsLearned = fresh.signsLearned.toLong(),
            lessonsCompleted = fresh.lessonsCompleted.toLong(),
            badgesEarned = fresh.badgesEarned.toLong(),
            lastSessionAt = null,
        )
    }

    /**
     * Streak math using local-day boundaries:
     * - same local day as last session → streak unchanged
     * - previous local day             → streak += 1
     * - any earlier                    → streak resets to 1
     */
    private fun calculateNewStreak(
        previousStreak: Int,
        lastSessionAt: Long?,
        nowMillis: Long,
    ): Int {
        if (lastSessionAt == null) return 1
        val tz = TimeZone.currentSystemDefault()
        val lastDay = Instant.fromEpochMilliseconds(lastSessionAt).toLocalDateTime(tz).date
        val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(tz).date
        val gap = lastDay.daysUntil(today)
        return when (gap) {
            0 -> previousStreak.coerceAtLeast(1)
            1 -> previousStreak + 1
            else -> 1
        }
    }

    private fun ProgressStatsEntity.toDomain(): ProgressStats = ProgressStats(
        streakDays = streakDays.toInt(),
        personalBestStreak = personalBestStreak.toInt(),
        totalXp = totalXp.toInt(),
        signsLearned = signsLearned.toInt(),
        lessonsCompleted = lessonsCompleted.toInt(),
        badgesEarned = badgesEarned.toInt(),
        // weeklyHeights/weeklyLabels are UI-computed — repo doesn't store them.
        weeklyHeights = List(7) { 0f },
        weeklyLabels = listOf("M", "T", "W", "T", "F", "S", "S"),
    )

    /**
     * Zeros with sensible week labels. Kept private — callers get this via
     * [observe] / [get] when the row is missing rather than constructing directly.
     */
    private fun default(): ProgressStats = ProgressStats(
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
