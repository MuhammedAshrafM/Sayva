package org.moashraf.sayva.data.repository

import kotlinx.coroutines.flow.Flow
import org.moashraf.sayva.data.FavoritePhrase

/**
 * Consumer of `FavoritesScreen`. Quick-access phrases the user has saved for TTS.
 *
 * Persistence: backed by SQLDelight `FavoritePhraseEntity`. Colors are stored as
 * JSON string in the DB; conversion to `androidx.compose.ui.graphics.Color` is
 * done in the mapping layer.
 */
interface FavoritesRepository {

    /** All phrases in user-defined sort order. */
    fun observeAll(): Flow<List<FavoritePhrase>>

    /** Filtered by category name (case-sensitive match on the mock data values). */
    fun observeByCategory(category: String): Flow<List<FavoritePhrase>>

    /** Only phrases flagged as emergency — used by the emergency-mode banner. */
    fun observeEmergency(): Flow<List<FavoritePhrase>>

    /** Load a single phrase by id. */
    suspend fun getById(id: String): FavoritePhrase?

    /** Add a new phrase or replace an existing one with the same id. */
    suspend fun upsert(phrase: FavoritePhrase)

    /** Reorder a phrase to a new position. Other phrases' order stays stable. */
    suspend fun setSortOrder(id: String, sortOrder: Int)

    /** Delete a single phrase. No-op if not present. */
    suspend fun delete(id: String)
}
