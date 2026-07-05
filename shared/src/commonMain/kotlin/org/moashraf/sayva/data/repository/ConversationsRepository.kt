package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.Flow
import org.moashraf.sayva.data.SavedConversation

/**
 * Consumer of `SavedConversationsScreen` and `ConversationScreen`.
 *
 * Persistence: backed by SQLDelight `SavedConversationEntity`. Full transcript
 * message rows are NOT stored here — they belong to a separate table added when
 * conversation-content persistence is wired (deferred to a later ticket).
 */
interface ConversationsRepository {

    /** All saved conversations newest-first. */
    fun observeAll(): Flow<List<SavedConversation>>

    /** Only conversations marked as favorite. */
    fun observeFavorites(): Flow<List<SavedConversation>>

    /** Load a single conversation by id. */
    suspend fun getById(id: String): SavedConversation?

    /** Insert or replace. Called by `ConversationScreen`'s "Stop & save" action. */
    suspend fun upsert(conversation: SavedConversation)

    /** Toggle the favorite flag. */
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    /** Toggle the highlighted (pinned) flag. */
    suspend fun setHighlighted(id: String, highlighted: Boolean)

    /** Delete a single conversation. */
    suspend fun delete(id: String)

    /** Wipe all — used by "clear data" flow. */
    suspend fun clear()
}
