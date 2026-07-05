package org.moashraf.sayva.ui.screens.memory

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.moashraf.sayva.data.HistoryDetail
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.OnWarningContainer
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary50
import org.moashraf.sayva.designsystem.SecondaryContainer
import org.moashraf.sayva.designsystem.SuccessContainer
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.WarningColor
import org.moashraf.sayva.designsystem.WarningContainer
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.speech.speakText
import org.moashraf.sayva.designsystem.OnPrimaryContainer
import org.moashraf.sayva.designsystem.OnSecondaryContainer
import org.moashraf.sayva.designsystem.OnTertiaryContainer
import org.moashraf.sayva.ui.viewmodel.HistoryViewModel

private val fallbackDetail = HistoryDetail(
    id = "unknown",
    recognizedSign = "Translation not found",
    confidence = 0,
    language = "—",
    timeLabel = "—",
    spokenAs = "\"…\"",
)

@Composable
fun HistoryDetailScreen(nav: SayvaNavController, entryId: String) {
    val viewModel: HistoryViewModel = koinInject()

    // Detail loaded once per entryId — the row's content is immutable
    // (recognized sign, confidence, timestamps don't change after write).
    var detail by remember(entryId) { mutableStateOf<HistoryDetail?>(null) }
    var isFavorite by remember(entryId) { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        detail = viewModel.getDetail(entryId)
        isFavorite = viewModel.getItem(entryId)?.isFavorite ?: false
    }

    val visibleDetail = detail ?: fallbackDetail

    Column(modifier = Modifier.fillMaxSize()) {
        // App bar.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(SurfaceContainer, CircleShape)
                    .clickable { nav.back() },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "arrow_back", size = 18.dp, color = OnSurface)
            }
            Text(
                "Translation",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(SurfaceContainer, CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "share", size = 18.dp, color = OnSurface)
            }
        }

        // Replay video placeholder.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(240.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF3A3550), Color(0xFF1A1B25))), RoundedCornerShape(20.dp)),
        ) {
            SymbolIcon(
                name = "sign_language",
                size = 120.dp,
                color = Color.White.copy(alpha = 0.25f),
                filled = true,
                modifier = Modifier.align(Alignment.Center),
            )
            Text(
                "REPLAY · 0:03",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.95f), CircleShape)
                        .clickable { speakText(visibleDetail.spokenAs) },
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "play_arrow", size = 18.dp, color = OnSurface, filled = true)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.3f).height(4.dp).background(Color.White, RoundedCornerShape(2.dp)))
                }
                Spacer(Modifier.width(8.dp))
                Text("0:01 / 0:03", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }

        // Detail text block.
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)) {
            Text("RECOGNIZED SIGN", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(visibleDetail.recognizedSign, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${visibleDetail.confidence}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = OnTertiaryContainer,
                    modifier = Modifier.background(SuccessContainer, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text(
                    visibleDetail.language,
                    style = MaterialTheme.typography.labelMedium,
                    color = OnPrimaryContainer,
                    modifier = Modifier.background(PrimaryContainer, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Text(
                    visibleDetail.timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = OnSurfaceVariant,
                    modifier = Modifier.background(SurfaceContainer, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainer, RoundedCornerShape(14.dp))
                    .padding(14.dp),
            ) {
                Text("SPOKEN AS", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Secondary50, RoundedCornerShape(12.dp))
                            .clickable { speakText(visibleDetail.spokenAs) },
                        contentAlignment = Alignment.Center,
                    ) {
                        SymbolIcon(name = "volume_up", size = 18.dp, color = Color.White, filled = true)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(visibleDetail.spokenAs, style = MaterialTheme.typography.bodyMedium, color = OnSurface, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom action row.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ActionTile(
                icon = "delete",
                label = "Delete",
                iconColor = Secondary50,
                bg = SecondaryContainer,
                textColor = OnSecondaryContainer,
                onClick = {
                    viewModel.delete(entryId)
                    nav.back()
                },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = if (isFavorite) "star" else "star_outline",
                label = "Favorite",
                iconColor = WarningColor,
                bg = WarningContainer,
                textColor = OnWarningContainer,
                onClick = {
                    viewModel.toggleFavorite(entryId, isFavorite)
                    isFavorite = !isFavorite
                },
                modifier = Modifier.weight(1f),
                filled = true,
            )
            ActionTile(
                icon = "edit",
                label = "Correct",
                iconColor = Primary40,
                bg = PrimaryContainer,
                textColor = OnPrimaryContainer,
                onClick = { },
                modifier = Modifier.weight(1f),
            )
            ActionTile(
                icon = "share",
                label = "Share",
                iconColor = Color.White,
                bg = OnSurface,
                textColor = Color.White,
                onClick = { },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: String,
    label: String,
    iconColor: Color,
    bg: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
) {
    Column(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SymbolIcon(name = icon, size = 20.dp, color = iconColor, filled = filled)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}
