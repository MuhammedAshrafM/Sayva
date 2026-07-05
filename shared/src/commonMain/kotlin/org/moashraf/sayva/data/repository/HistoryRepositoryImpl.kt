package org.moashraf.sayva.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.moashraf.sayva.data.HistoryDetail
import org.moashraf.sayva.data.HistoryItem
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.db.HistoryItemEntity
import org.moashraf.sayva.db.SayvaDatabase

/**
 * SQLDelight-backed [HistoryRepository].
 *
 * `Flow<List<HistoryItem>>` is produced by SQLDelight's coroutines extension:
 * every insert/update/delete against the underlying table invalidates the
 * query and re-emits. Collectors don't need to poll.
 *
 * ### Timestamps
 * The DB's `createdAt` column drives ordering. When the caller provides an
 * item without a persisted timestamp, we synthesize one via `Clock.System.now()`
 * so ordering stays sane. Real usage should always assign a real timestamp at
 * the moment the recognition happened.
 */
class HistoryRepositoryImpl(
    database: SayvaDatabase,
) : HistoryRepository {

    private val queries = database.historyItemQueries
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Seed sample entries on first launch so the UI has content out of the
        // box. Idempotent — subsequent launches see rows already present and skip.
        scope.launch { seedIfEmpty() }
    }

    private suspend fun seedIfEmpty() {
        if (queries.count().executeAsOne() > 0L) return
        // Use a monotonically-decreasing base timestamp so seeded entries sort
        // stably (newest first) without depending on the mock's `day`/`time` order.
        val baseTime = Clock.System.now().toEpochMilliseconds()
        MockSayvaData.history.forEachIndexed { index, item ->
            queries.insert(
                id = item.id,
                day = item.day,
                time = item.time,
                title = item.title,
                confidence = item.confidence?.toLong(),
                language = item.language,
                isFavorite = if (item.isFavorite) 1L else 0L,
                isConversation = if (item.isConversation) 1L else 0L,
                messageCount = item.messageCount?.toLong(),
                duration = item.duration,
                isLowConfidence = if (item.isLowConfidence) 1L else 0L,
                createdAt = baseTime - index * 1000L,
            )
        }
    }

    override fun observeAll(): Flow<List<HistoryItem>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeFavorites(): Flow<List<HistoryItem>> =
        queries.selectFavorites().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeByLanguage(language: String): Flow<List<HistoryItem>> =
        queries.selectByLanguage(language).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun getById(id: String): HistoryItem? =
        queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun getDetail(id: String): HistoryDetail? {
        // Detail info lives on the same row today; when we add a separate
        // detail table this becomes a join.
        val row = queries.selectById(id).executeAsOneOrNull() ?: return null
        return HistoryDetail(
            id = row.id,
            recognizedSign = row.title,
            confidence = row.confidence?.toInt() ?: 0,
            language = row.language ?: "ASL",
            timeLabel = "${row.day} · ${row.time}",
            spokenAs = row.title,
        )
    }

    override suspend fun upsert(item: HistoryItem) {
        queries.insert(
            id = item.id,
            day = item.day,
            time = item.time,
            title = item.title,
            confidence = item.confidence?.toLong(),
            language = item.language,
            isFavorite = if (item.isFavorite) 1L else 0L,
            isConversation = if (item.isConversation) 1L else 0L,
            messageCount = item.messageCount?.toLong(),
            duration = item.duration,
            isLowConfidence = if (item.isLowConfidence) 1L else 0L,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        queries.updateFavorite(isFavorite = if (isFavorite) 1L else 0L, id = id)
    }

    override suspend fun delete(id: String) {
        queries.delete(id)
    }

    override suspend fun clear() {
        queries.deleteAll()
    }

    override suspend fun count(): Long = queries.count().executeAsOne()

    // -----------------------------------------------------------------

    private fun HistoryItemEntity.toDomain(): HistoryItem = HistoryItem(
        id = id,
        day = day,
        time = time,
        title = title,
        confidence = confidence?.toInt(),
        language = language,
        isFavorite = isFavorite == 1L,
        isConversation = isConversation == 1L,
        messageCount = messageCount?.toInt(),
        duration = duration,
        isLowConfidence = isLowConfidence == 1L,
    )
}
