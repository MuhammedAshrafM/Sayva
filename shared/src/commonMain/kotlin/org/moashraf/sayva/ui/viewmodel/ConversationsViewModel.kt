package org.moashraf.sayva.ui.viewmodel

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.moashraf.sayva.data.SavedConversation
import org.moashraf.sayva.data.repository.ConversationsRepository
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.telemetry.AnalyticsEvents
import org.moashraf.sayva.telemetry.AnalyticsGateway

/**
 * Feeds both `SavedConversationsScreen` (the list) and `ConversationScreen`
 * (the live/save action). Keeping them under one VM keeps a single hot cache
 * of conversations in memory — the list re-renders instantly when "Stop &
 * save" upserts a row on the other screen.
 */
class ConversationsViewModel(
    private val repository: ConversationsRepository,
    private val analytics: AnalyticsGateway,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** All conversations, newest-first. Hot flow shared by any UI observer. */
    val allConversations: StateFlow<List<SavedConversation>> =
        repository.observeAll()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Only favorites — used later if `SavedConversationsScreen` grows a filter. */
    val favoriteConversations: StateFlow<List<SavedConversation>> =
        repository.observeFavorites()
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** Toggle the star on the SavedConversations list. */
    fun toggleFavorite(id: String, isFavorite: Boolean) {
        analytics.logEvent(
            AnalyticsEvents.CONVERSATION_FAVORITE_TOGGLED,
            mapOf(
                AnalyticsEvents.Param.CONVERSATION_ID to id,
                AnalyticsEvents.Param.VALUE to isFavorite,
            ),
        )
        scope.launch { repository.setFavorite(id, isFavorite) }
    }

    /**
     * "Stop & save" from `ConversationScreen`. We synthesize the SavedConversation
     * metadata from what's currently on screen — the transcript itself isn't
     * persisted yet (per repo doc, that lands with a later ticket that adds a
     * conversation-message table).
     *
     * `partnerColor` defaults to Primary40 for now; a picker would let the user
     * choose the color that appears on the SavedConversations card avatar.
     */
    fun saveConversation(
        title: String,
        preview: String,
        partnerInitial: String,
        durationLabel: String,
        messageCount: Int?,
        category: String?,
        partnerColor: Color = Primary40,
    ) {
        scope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            val id = "conv-$now"
            repository.upsert(
                SavedConversation(
                    id = id,
                    title = title,
                    timeLabel = "Today · $durationLabel",
                    preview = preview,
                    partnerInitial = partnerInitial,
                    partnerColor = partnerColor,
                    messageCount = messageCount,
                    category = category,
                ),
            )
            // Log after the upsert succeeds so failures (which would throw and
            // be caught up-stream) don't produce false-positive "saved" events.
            // NOTE: the raw preview text is intentionally NOT logged — that's
            // conversation content and belongs on-device only.
            analytics.logEvent(
                AnalyticsEvents.CONVERSATION_SAVED,
                buildMap<String, Any> {
                    put(AnalyticsEvents.Param.CONVERSATION_ID, id)
                    messageCount?.let { put(AnalyticsEvents.Param.MESSAGE_COUNT, it) }
                    category?.let { put(AnalyticsEvents.Param.CATEGORY, it) }
                },
            )
        }
    }

    fun delete(id: String) {
        scope.launch { repository.delete(id) }
    }
}
