package org.moashraf.sayva.ui.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.moashraf.sayva.data.HistoryDetail
import org.moashraf.sayva.data.HistoryItem
import org.moashraf.sayva.data.repository.HistoryRepository
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway

/**
 * Feeds `HistoryScreen` and `HistoryDetailScreen`. Combines the repository's
 * Flow of all entries with a local filter state so filter chip taps re-emit
 * the filtered list without a round-trip through the DB.
 *
 * Server-side filtering (SQL WHERE clauses) would work too, but with the row
 * count we expect (<10k lifetime per user) in-memory filtering is simpler and
 * avoids one query per filter change.
 */
class HistoryViewModel(
    private val repository: HistoryRepository,
    private val analytics: AnalyticsGateway,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _filter = MutableStateFlow(HistoryFilter.All)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    /** All entries, filtered by [filter]. Empty until first repo emission. */
    val visibleHistory: StateFlow<List<HistoryItem>> = combine(
        repository.observeAll(),
        _filter,
    ) { items, activeFilter ->
        applyFilter(items, activeFilter)
    }.stateIn(
        scope = scope,
        // Drop the collector 5 seconds after the last subscriber leaves so the
        // upstream DB Flow can be garbage-collected during long backgrounds.
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
    )

    fun setFilter(newFilter: HistoryFilter) {
        if (_filter.value == newFilter) return
        _filter.value = newFilter
        analytics.logEvent(
            AnalyticsEvents.HISTORY_FILTER_CHANGED,
            mapOf(AnalyticsEvents.Param.FILTER to newFilter.name.lowercase()),
        )
    }

    suspend fun getDetail(id: String): HistoryDetail? = repository.getDetail(id)

    suspend fun getItem(id: String): HistoryItem? = repository.getById(id)

    fun toggleFavorite(id: String, isFavoriteNow: Boolean) {
        scope.launch { repository.setFavorite(id, !isFavoriteNow) }
    }

    fun delete(id: String) {
        scope.launch { repository.delete(id) }
    }

    private fun applyFilter(
        items: List<HistoryItem>,
        f: HistoryFilter,
    ): List<HistoryItem> = when (f) {
        HistoryFilter.All -> items
        HistoryFilter.Today -> items.filter { it.day.equals("TODAY", ignoreCase = true) }
        HistoryFilter.Favorites -> items.filter { it.isFavorite }
        HistoryFilter.HighConfidence -> items.filter { (it.confidence ?: 0) >= 90 }
    }
}

/**
 * Filter chips shown at the top of HistoryScreen. Order in this enum determines
 * chip order in the UI (see `HistoryFilter.entries.forEach { ... }`).
 *
 * A per-Pack "language" filter is deliberately absent: language-neutrality
 * principle #1 forbids hardcoded language filters. When we ship multi-pack
 * history (post-Phase 4), the filter will be sourced dynamically from
 * `LanguagePackRegistry.installed` — every installed Pack contributes a
 * chip; adding a Pack does not touch this file.
 */
enum class HistoryFilter(val label: String) {
    All("All"),
    Today("Today"),
    Favorites("★ Favs"),
    HighConfidence("High"),
}
