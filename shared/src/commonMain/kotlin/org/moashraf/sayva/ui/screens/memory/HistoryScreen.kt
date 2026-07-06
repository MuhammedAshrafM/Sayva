package org.moashraf.sayva.ui.screens.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.ui.components.a11yTabRow
import org.moashraf.sayva.data.HistoryItem
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.designsystem.WarningContainer
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.ui.viewmodel.HistoryFilter
import org.moashraf.sayva.ui.viewmodel.HistoryViewModel

@Composable
fun HistoryScreen(nav: SayvaNavController) {
    val viewModel: HistoryViewModel = koinInject()
    val selectedFilter by viewModel.filter.collectAsState()
    val visibleHistory by viewModel.visibleHistory.collectAsState()
    val grouped = visibleHistory.groupBy { it.day }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = "arrow_back", label = "Back", onClick = { nav.back() })
                Text(
                    "History",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                CircleIconButton(icon = "more_vert", label = "More options", onClick = {})
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(SurfaceContainer, RoundedCornerShape(100))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "search", size = 18.dp, color = OnSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Search translations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                SymbolIcon(name = "mic", size = 18.dp, color = Primary40)
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(HistoryFilter.entries.toList(), key = { it.name }) { filter ->
                    val selected = filter == selectedFilter
                    Text(
                        filter.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else OnSurface,
                        modifier = Modifier
                            .background(if (selected) Primary40 else Color.White, RoundedCornerShape(8.dp))
                            .border(if (selected) 0.dp else 1.dp, OutlineStrong, RoundedCornerShape(8.dp))
                            .a11yTabRow(label = filter.label, isSelected = selected) {
                                viewModel.setFilter(filter)
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                grouped.forEach { (day, items) ->
                    item {
                        Text(
                            day,
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                        )
                    }
                    items(items, key = { it.id }) { entry ->
                        HistoryRow(entry = entry, onClick = { nav.navigate(Screen.HistoryDetail(entry.id)) })
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp)
                .size(56.dp)
                .background(Primary40, RoundedCornerShape(18.dp))
                .clickable(role = Role.Button, onClickLabel = "Start new translation") {
                    nav.navigate(Screen.LiveCamera)
                }
                .semantics { contentDescription = "New translation" },
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = "videocam", size = 28.dp, color = Color.White, filled = true, contentDescription = null)
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryItem, onClick: () -> Unit) {
    val (iconBg, iconColor, icon) = when {
        entry.isConversation -> Triple(SecondaryContainer, Secondary50, "forum")
        entry.isLowConfidence -> Triple(WarningContainer, WarningColor, "help")
        else -> Triple(PrimaryContainer, Primary40, "sign_language")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .a11yButtonRow(label = "${entry.title}, ${entry.time}") { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = icon, size = 18.dp, color = iconColor, filled = true)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall)
            val subtitle = buildList {
                add(entry.time)
                entry.confidence?.let { add("$it%") }
                entry.duration?.let { add(it) }
                entry.language?.let { add(it) }
            }.joinToString(" · ")
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        when {
            entry.isConversation -> SymbolIcon(name = "chevron_right", size = 18.dp, color = OnSurfaceVariant)
            entry.isFavorite -> SymbolIcon(name = "star", size = 18.dp, color = WarningColor, filled = true)
            else -> SymbolIcon(name = "star_outline", size = 18.dp, color = OnSurfaceVariant)
        }
    }
}

@Composable
private fun CircleIconButton(icon: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(SurfaceContainer, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(name = icon, size = 18.dp, color = OnSurface, contentDescription = null)
    }
}
