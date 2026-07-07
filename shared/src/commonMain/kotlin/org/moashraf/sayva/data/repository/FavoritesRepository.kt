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

    // -----------------------------------------------------------------------
    // Live-recognition provenance
    //
    // Recognized signs from LiveCameraScreen become FavoritePhrase rows with
    // a deterministic id derived from (packCode, signId). This lets the star
    // button toggle a favorite without inventing a new table or coupling the
    // UI to persistence details — everything reuses `upsert` + `delete`.
    //
    // The scheme keeps FavoritesRepository language-pack agnostic: any pack
    // that produces (packCode, signId, label) tuples can favorite through
    // this API.
    // -----------------------------------------------------------------------

    /**
     * Deterministic id for a favorite that originated from a live recognition
     * of `signId` under `packCode`. Callers use this to check state or delete
     * without needing the phrase itself.
     */
    fun favoriteIdForSign(packCode: String, signId: String): String

    /**
     * Add or remove a favorite representing a live-recognition result.
     * Idempotent within each direction — repeated calls that both mean
     * "favorite this" end up with one row; repeated "unfavorite" calls
     * leave the row absent.
     *
     * @return `true` if the sign is favorited after this call, `false`
     *   otherwise.
     */
    suspend fun toggleFavoriteFromSign(
        packCode: String,
        signId: String,
        label: String,
    ): Boolean
}
