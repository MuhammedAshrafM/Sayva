package org.moashraf.sayva.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import org.moashraf.sayva.data.FavoritePhrase
import org.moashraf.sayva.data.MockSayvaData
import org.moashraf.sayva.db.FavoritePhraseEntity
import org.moashraf.sayva.db.SayvaDatabase
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Tertiary50

/**
 * SQLDelight-backed [FavoritesRepository]. Gradients are encoded as pipe-separated
 * ARGB hex strings via [encodeGradient] / [decodeGradient] — see [ColorMappers].
 *
 * ### Sort order semantics
 * The [FavoritePhrase] domain model doesn't carry `sortOrder`. On [upsert] we
 * either preserve the row's existing sort order (if it exists) or append to the
 * end (next-count) if it's new. Explicit reordering goes through [setSortOrder].
 */
class FavoritesRepositoryImpl(
    database: SayvaDatabase,
) : FavoritesRepository {

    private val queries = database.favoritePhraseQueries
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch { seedIfEmpty() }
    }

    private suspend fun seedIfEmpty() {
        if (queries.count().executeAsOne() > 0L) return
        MockSayvaData.favorites.forEachIndexed { index, phrase ->
            queries.insert(
                id = phrase.id,
                category = phrase.category,
                text = phrase.text,
                gradientColorsJson = phrase.gradient.encodeGradient(),
                icon = phrase.icon,
                isEmergency = if (phrase.isEmergency) 1L else 0L,
                sortOrder = index.toLong(),
            )
        }
    }

    override fun observeAll(): Flow<List<FavoritePhrase>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeByCategory(category: String): Flow<List<FavoritePhrase>> =
        queries.selectByCategory(category).asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override fun observeEmergency(): Flow<List<FavoritePhrase>> =
        queries.selectEmergency().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun getById(id: String): FavoritePhrase? =
        queries.selectById(id).executeAsOneOrNull()?.toDomain()

    override suspend fun upsert(phrase: FavoritePhrase) {
        val existingSortOrder = queries.selectById(phrase.id).executeAsOneOrNull()?.sortOrder
        val sortOrder = existingSortOrder ?: queries.count().executeAsOne()
        queries.insert(
            id = phrase.id,
            category = phrase.category,
            text = phrase.text,
            gradientColorsJson = phrase.gradient.encodeGradient(),
            icon = phrase.icon,
            isEmergency = if (phrase.isEmergency) 1L else 0L,
            sortOrder = sortOrder,
        )
    }

    override suspend fun setSortOrder(id: String, sortOrder: Int) {
        queries.updateSortOrder(sortOrder = sortOrder.toLong(), id = id)
    }

    override suspend fun delete(id: String) {
        queries.delete(id)
    }

    // -----------------------------------------------------------------
    // Live-recognition provenance
    // -----------------------------------------------------------------

    /**
     * Deterministic id prefix that reserves the `sign:` namespace for
     * favorites created from live recognition. Curated / seed favorites
     * (from MockSayvaData) use short ids like `"f1"`, `"f2"` — never
     * collide.
     */
    private companion object {
        const val SIGN_ID_PREFIX = "sign"
        const val RECOGNIZED_CATEGORY = "RECOGNIZED"
        const val RECOGNIZED_ICON = "auto_awesome"
    }

    override fun favoriteIdForSign(packCode: String, signId: String): String =
        "$SIGN_ID_PREFIX:$packCode:$signId"

    override suspend fun toggleFavoriteFromSign(
        packCode: String,
        signId: String,
        label: String,
    ): Boolean {
        val id = favoriteIdForSign(packCode, signId)
        val existing = queries.selectById(id).executeAsOneOrNull()
        return if (existing != null) {
            queries.delete(id)
            false
        } else {
            upsert(
                FavoritePhrase(
                    id = id,
                    category = RECOGNIZED_CATEGORY,
                    text = label,
                    gradient = defaultRecognitionGradient(),
                    icon = RECOGNIZED_ICON,
                    isEmergency = false,
                ),
            )
            true
        }
    }

    /** Two-stop gradient reused across every recognition-sourced favorite.
     *  Kept language-neutral (no pack-provided colors) — packs can grow their
     *  own theming in a later iteration without changing this repository. */
    private fun defaultRecognitionGradient(): List<Color> =
        listOf(Primary40, Tertiary50)

    // -----------------------------------------------------------------

    private fun FavoritePhraseEntity.toDomain(): FavoritePhrase = FavoritePhrase(
        id = id,
        category = category,
        text = text,
        gradient = gradientColorsJson.decodeGradient(),
        icon = icon,
        isEmergency = isEmergency == 1L,
    )
}
