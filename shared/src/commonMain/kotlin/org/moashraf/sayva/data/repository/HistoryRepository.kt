package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.Flow
import org.moashraf.sayva.data.HistoryItem
import org.moashraf.sayva.data.HistoryDetail

/**
 * Consumer of `HistoryScreen` and `HistoryDetailScreen`.
 *
 * Persistence: backed by SQLDelight `HistoryItemEntity` table. Implementation
 * lands in a follow-up ticket — the interface exists now so ViewModels can be
 * written against it in parallel.
 */
interface HistoryRepository {

    /** All entries newest-first. Emits again on any change. */
    fun observeAll(): Flow<List<HistoryItem>>

    /** Only entries marked as favorite. */
    fun observeFavorites(): Flow<List<HistoryItem>>

    /** Entries filtered by language (ASL, BSL, etc.). */
    fun observeByLanguage(language: String): Flow<List<HistoryItem>>

    /** Load a single entry by id — null if not found (deleted, or bad link). */
    suspend fun getById(id: String): HistoryItem?

    /** Detail view — extended metadata for a single entry. */
    suspend fun getDetail(id: String): HistoryDetail?

    /** Insert a new entry or replace an existing one with the same id. */
    suspend fun upsert(item: HistoryItem)

    /** Toggle the favorite flag. Idempotent. */
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /** Delete a single entry. No-op if not present. */
    suspend fun delete(id: String)

    /** Wipe all history — used by settings' "clear data" action. */
    suspend fun clear()

    /** Count of entries, cheap query. */
    suspend fun count(): Long
}
