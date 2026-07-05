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
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.data.SavedConversation
import org.moashraf.sayva.db.SavedConversationEntity
import org.moashraf.sayva.db.SayvaDatabase

/**
 * SQLDelight-backed [ConversationsRepository].
 *
 * Only `partnerColor` is persisted as a hex string; `categoryColor` and
 * `categoryBg` are derived from the `category` string at render time (they
 * default in the domain model). This keeps the schema narrow — colors that
 * are category-derived should stay category-derived, not duplicated per row.
 *
 * ### First-launch seed
 * If the table is empty we seed from [MockSayvaData.savedConversations] so a
 * fresh install still shows sample content on `SavedConversationsScreen`. Real
 * conversations saved via "Stop & save" upsert alongside the seed rows.
 */
class ConversationsRepositoryImpl(
    database: SayvaDatabase,
) : ConversationsRepository {

    private val queries = database.savedConversationQueries
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch { seedIfEmpty() }
    }

    private suspend fun seedIfEmpty() {
        if (queries.count().executeAsOne() > 0L) return
        // Ordered by index descending so newer-looking entries (index 0) end up
        // with the most-recent createdAt — matches the design's "Today" first.
        val now = Clock.System.now().toEpochMilliseconds()
        MockSayvaData.savedConversations.forEachIndexed { index, c ->
            queries.insert(
                id = c.id,
                title = c.title,
                timeLabel = c.timeLabel,
                preview = c.preview,
                partnerInitial = c.partnerInitial,
                partnerColorHex = c.partnerColor.toHexArgb(),
                messageCount = c.messageCount?.toLong(),
                category = c.category,
                isFavorite = if (c.isFavorite) 1L else 0L,
                highlighted = if (c.highlighted) 1L else 0L,
                createdAt = now - index * 60_000L,
            )
        }
    }

    override fun observeAll(): Flow<List<SavedConversation>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeFavorites(): Flow<List<SavedConversation>> =
        queries.selectFavorites().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun getById(id: String): SavedConversation? =
        queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun upsert(conversation: SavedConversation) {
        queries.insert(
            id = conversation.id,
            title = conversation.title,
            timeLabel = conversation.timeLabel,
            preview = conversation.preview,
            partnerInitial = conversation.partnerInitial,
            partnerColorHex = conversation.partnerColor.toHexArgb(),
            messageCount = conversation.messageCount?.toLong(),
            category = conversation.category,
            isFavorite = if (conversation.isFavorite) 1L else 0L,
            highlighted = if (conversation.highlighted) 1L else 0L,
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    override suspend fun setFavorite(id: String, isFavorite: Boolean) {
        queries.updateFavorite(isFavorite = if (isFavorite) 1L else 0L, id = id)
    }

    override suspend fun setHighlighted(id: String, highlighted: Boolean) {
        queries.updateHighlighted(highlighted = if (highlighted) 1L else 0L, id = id)
    }

    override suspend fun delete(id: String) {
        queries.delete(id)
    }

    override suspend fun clear() {
        queries.deleteAll()
    }

    // -----------------------------------------------------------------

    private fun SavedConversationEntity.toDomain(): SavedConversation = SavedConversation(
        id = id,
        title = title,
        timeLabel = timeLabel,
        preview = preview,
        partnerInitial = partnerInitial,
        partnerColor = partnerColorHex.toColorFromHex(),
        messageCount = messageCount?.toInt(),
        category = category,
        isFavorite = isFavorite == 1L,
        highlighted = highlighted == 1L,
        // categoryColor / categoryBg default from the SavedConversation constructor.
        // They're derived from `category` at render time, not stored per row.
    )
}
