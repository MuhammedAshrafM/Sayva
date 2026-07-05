package org.moashraf.sayva.ui.screens.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.koin.compose.koinInject
import org.moashraf.sayva.data.SavedConversation
import org.moashraf.sayva.ui.components.a11yButtonRow
import org.moashraf.sayva.ui.viewmodel.ConversationsViewModel
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Outline
import org.moashraf.sayva.designsystem.OutlineStrong
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Primary80
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.SurfaceDim
import org.moashraf.sayva.designsystem.Surface
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.nav.Screen
import org.moashraf.sayva.nav.SayvaNavController

@Composable
fun SavedConversationsScreen(nav: SayvaNavController) {
    val viewModel: ConversationsViewModel = koinInject()
    val conversations by viewModel.allConversations.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(icon = "arrow_back", label = "Back", onClick = { nav.back() })
                Text(
                    "Saved conversations",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                CircleIconButton(icon = "filter_list", label = "Filter conversations", onClick = {})
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .background(SurfaceDim, RoundedCornerShape(100))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "search", size = 18.dp, color = OnSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Search transcripts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariant,
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { nav.navigate(Screen.Conversation) },
                        onFavoriteToggle = {
                            viewModel.toggleFavorite(conversation.id, !conversation.isFavorite)
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 24.dp)
                .background(OnSurface, RoundedCornerShape(18.dp))
                .a11yButtonRow(label = "Start new chat") { nav.navigate(Screen.Conversation) }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "add", size = 20.dp, color = Color.White, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("New chat", style = MaterialTheme.typography.titleSmall, color = Color.White)
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: SavedConversation,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val containerModifier = if (conversation.highlighted) {
        Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(PrimaryContainer, Surface)), RoundedCornerShape(18.dp))
            .border(1.dp, Primary80, RoundedCornerShape(18.dp))
    } else {
        Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .border(1.dp, Outline, RoundedCornerShape(18.dp))
    }

    Column(
        modifier = containerModifier
            .a11yButtonRow(label = "${conversation.title}, ${conversation.timeLabel}") { onClick() }
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
            Box {
                Box(
                    modifier = Modifier.size(32.dp).background(Primary40, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("J", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
                Box(
                    modifier = Modifier
                        .padding(start = 22.dp)
                        .size(32.dp)
                        .background(conversation.partnerColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(conversation.partnerInitial, style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(conversation.title, style = MaterialTheme.typography.titleSmall)
                Text(conversation.timeLabel, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
            }
            SymbolIcon(
                name = if (conversation.isFavorite) "star" else "star_outline",
                size = 18.dp,
                color = if (conversation.isFavorite) WarningColor else OnSurfaceVariant,
                filled = conversation.isFavorite,
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = if (conversation.isFavorite) "Unfavorite" else "Favorite",
                    onClick = onFavoriteToggle,
                ),
                contentDescription = if (conversation.isFavorite) "Favorited" else "Not favorited",
            )
        }
        Text(
            conversation.preview,
            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
            color = OnSurfaceVariant,
        )
        if (conversation.messageCount != null || conversation.category != null) {
            Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                conversation.messageCount?.let {
                    Text(
                        "$it messages",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .border(1.dp, OutlineStrong, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                conversation.category?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = conversation.categoryColor,
                        modifier = Modifier
                            .background(conversation.categoryBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(icon: String, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(SurfaceDim, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        SymbolIcon(name = icon, size = 18.dp, color = OnSurface, contentDescription = null)
    }
}
