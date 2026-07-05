package org.moashraf.sayva.ui.screens.system

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.moashraf.sayva.designsystem.ErrorColor
import org.moashraf.sayva.designsystem.ErrorContainer
import org.moashraf.sayva.designsystem.OnSurface
import org.moashraf.sayva.designsystem.OnSurfaceVariant
import org.moashraf.sayva.designsystem.Primary40
import org.moashraf.sayva.designsystem.Primary60
import org.moashraf.sayva.designsystem.Primary80
import org.moashraf.sayva.designsystem.PrimaryContainer
import org.moashraf.sayva.designsystem.Secondary70
import org.moashraf.sayva.designsystem.SuccessColor
import org.moashraf.sayva.designsystem.SurfaceContainer
import org.moashraf.sayva.designsystem.SymbolIcon
import org.moashraf.sayva.designsystem.Tertiary50
import org.moashraf.sayva.nav.SayvaNavController
import org.moashraf.sayva.designsystem.OnTertiaryContainer

@Composable
fun SystemStatesScreen(nav: SayvaNavController) {
    Column(modifier = Modifier.fillMaxSize()) {
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
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text("System states", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Empty · Loading · Error · Success",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item { StateSectionLabel("EMPTY · NO HISTORY") }
            item { EmptyStateCard() }

            item { StateSectionLabel("LOADING · AI MODEL WARM-UP") }
            item { LoadingStateCard() }

            item { StateSectionLabel("ERROR · CAMERA PERMISSION") }
            item { ErrorStateCard() }

            item { StateSectionLabel("SUCCESS · LESSON COMPLETE") }
            item { SuccessStateCard() }

            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SavedToast(modifier = Modifier.weight(1f))
                    OfflineToast(modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StateSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = OnSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 10.dp),
    )
}

@Composable
private fun EmptyStateCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceContainer, RoundedCornerShape(18.dp))
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Brush.linearGradient(listOf(PrimaryContainer, Primary80)), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            SymbolIcon(name = "history_toggle_off", size = 48.dp, color = Primary40)
        }
        Spacer(Modifier.height(16.dp))
        Text("Nothing here yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "Your first translation will appear here. Tap the camera below to start.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.width(240.dp),
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .background(Primary40, RoundedCornerShape(100))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolIcon(name = "videocam", size = 16.dp, color = Color.White, filled = true)
            Spacer(Modifier.width(6.dp))
            Text("Translate now", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
    }
}

@Composable
private fun LoadingStateCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OnSurface, RoundedCornerShape(18.dp))
            .padding(vertical = 32.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(88.dp).background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(74.dp).background(OnSurface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    SymbolIcon(name = "auto_awesome", size = 36.dp, color = Primary60, filled = true)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Loading AI model", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(
                "Warming up neural network · 78%",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(6.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(6.dp)
                        .background(Brush.linearGradient(listOf(Primary40, Primary60)), RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}

@Composable
private fun ErrorStateCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorContainer, RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFFBD7DD), RoundedCornerShape(18.dp))
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            Box(
                modifier = Modifier.size(48.dp).background(ErrorColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "no_photography", size = 24.dp, color = Color.White, filled = true)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Camera blocked",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF8C0D24),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sayva can't translate without seeing your hands. Open Settings to allow camera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8C0D24).copy(alpha = 0.85f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White, RoundedCornerShape(100))
                    .border(1.dp, ErrorColor, RoundedCornerShape(100))
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Try later", style = MaterialTheme.typography.labelMedium, color = ErrorColor)
            }
            Row(
                modifier = Modifier
                    .weight(1.5f)
                    .background(ErrorColor, RoundedCornerShape(100))
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SymbolIcon(name = "settings", size = 14.dp, color = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Open settings", style = MaterialTheme.typography.labelMedium, color = Color.White)
            }
        }
    }
}

@Composable
private fun SuccessStateCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(Tertiary50, Color(0xFF6BCFAB))), RoundedCornerShape(18.dp))
            .padding(vertical = 24.dp, horizontal = 20.dp),
    ) {
        SymbolIcon(
            name = "celebration",
            size = 120.dp,
            color = Color.White.copy(alpha = 0.18f),
            filled = true,
            modifier = Modifier.align(Alignment.TopEnd),
        )
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(72.dp).background(Color.White.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SymbolIcon(name = "check", size = 40.dp, color = Color.White, filled = true)
            }
            Spacer(Modifier.height(12.dp))
            Text("Lesson complete!", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                "+30 XP · streak day 13 unlocked 🔥",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(100))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Next lesson", style = MaterialTheme.typography.labelMedium, color = OnTertiaryContainer)
                Spacer(Modifier.width(6.dp))
                SymbolIcon(name = "arrow_forward", size = 14.dp, color = OnTertiaryContainer)
            }
        }
    }
}

@Composable
private fun SavedToast(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(OnSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(name = "check_circle", size = 18.dp, color = SuccessColor, filled = true)
        Spacer(Modifier.width(12.dp))
        Text(
            "Saved to favorites",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
        Text("UNDO", style = MaterialTheme.typography.labelMedium, color = Secondary70)
    }
}

@Composable
private fun OfflineToast(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(OnSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SymbolIcon(name = "cloud_off", size = 18.dp, color = Secondary70)
        Spacer(Modifier.width(12.dp))
        Text(
            "You're offline · using cached model",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )
    }
}
