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
import org.moashraf.sayva.data.FavoritePhrase
import org.moashraf.sayva.data.repository.FavoritesRepository
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway

/**
 * Feeds `FavoritesScreen`. Category filtering happens in-memory rather than via
 * `observeByCategory` — same reasoning as [HistoryViewModel]: small row counts,
 * simpler than juggling multiple upstream flows.
 *
 * Emergency-mode toggle lives on [org.moashraf.sayva.data.repository.SettingsRepository]
 * (via [SettingsViewModel]) — not here — because emergency mode is a global user
 * preference used across the app, not favorites-specific.
 */
class FavoritesViewModel(
    private val repository: FavoritesRepository,
    private val analytics: AnalyticsGateway,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _filter = MutableStateFlow(FavoriteFilter.All)
    val filter: StateFlow<FavoriteFilter> = _filter.asStateFlow()

    /** All favorite phrases in DB, unfiltered — used for total-count display. */
    val allFavorites: StateFlow<List<FavoritePhrase>> = repository.observeAll()
        .stateIn(scope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val visibleFavorites: StateFlow<List<FavoritePhrase>> = combine(
        repository.observeAll(),
        _filter,
    ) { phrases, activeFilter ->
        when (activeFilter) {
            FavoriteFilter.All -> phrases
            FavoriteFilter.Greetings -> phrases.filter { it.category.equals("Greetings", ignoreCase = true) }
            FavoriteFilter.Medical -> phrases.filter { it.category.equals("Medical", ignoreCase = true) }
            FavoriteFilter.Family -> phrases.filter { it.category.equals("Family", ignoreCase = true) }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setFilter(newFilter: FavoriteFilter) {
        if (_filter.value == newFilter) return
        _filter.value = newFilter
        analytics.logEvent(
            AnalyticsEvents.FAVORITE_FILTER_CHANGED,
            mapOf(AnalyticsEvents.Param.FILTER to newFilter.name.lowercase()),
        )
    }

    fun delete(id: String) {
        scope.launch { repository.delete(id) }
    }
}

/**
 * Category chips at the top of FavoritesScreen. Labels are hardcoded to English
 * for now; when we add localization we'll move these to string resources.
 */
enum class FavoriteFilter(val label: String) {
    All("All"),
    Greetings("Greetings"),
    Medical("Medical"),
    Family("Family"),
}
